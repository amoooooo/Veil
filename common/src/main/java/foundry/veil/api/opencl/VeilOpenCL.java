package foundry.veil.api.opencl;

import foundry.veil.api.client.render.VeilRenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL11.CL_DEVICE_OPENCL_C_VERSION;
import static org.lwjgl.opencl.CL12.CL_DEVICE_PREFERRED_INTEROP_USER_SYNC;
import static org.lwjgl.opencl.KHRICD.CL_PLATFORM_ICD_SUFFIX_KHR;

/**
 * Veil implementation of OpenCL.
 *
 * @author Ocelot
 */
public final class VeilOpenCL implements NativeResource {

    public static final Logger LOGGER = LoggerFactory.getLogger("Veil OpenCL");

    // Prefer platforms that support GPU devices
    private static final Comparator<DeviceInfo> COMPUTE_ORDER = (p1, p2) -> {
        boolean gpu1 = p1.isGpu();
        boolean gpu2 = p2.isGpu();
        return gpu1 == gpu2 ? 0 : (gpu1 ? -1 : 1);
    };

    private static VeilOpenCL instance;

    private final Map<DeviceInfo, CLEnvironment> environments;
    private final Set<DeviceInfo> invalidDevices;
    private final PlatformInfo[] platforms;
    private final List<DeviceInfo> priorityDevices;
    private final Lock deviceLock;

    private VeilOpenCL() {
        this.environments = new Object2ObjectArrayMap<>();
        this.invalidDevices = ConcurrentHashMap.newKeySet();

        PlatformInfo[] platforms;
        List<DeviceInfo> priorityDevices = new ArrayList<>();
        try {
            platforms = VeilOpenCL.requestPlatforms();

            for (PlatformInfo platform : platforms) {
                priorityDevices.addAll(Arrays.asList(platform.devices()));
            }
            priorityDevices.sort(COMPUTE_ORDER);
        } catch (Throwable t) {
            LOGGER.warn("Failed to load OpenCL", t);
            platforms = new PlatformInfo[0];
            priorityDevices.clear();
        }

        this.platforms = platforms;
        this.priorityDevices = Collections.unmodifiableList(priorityDevices);
        this.deviceLock = new ReentrantLock();
    }

    /**
     * @return The default OpenCL environment or <code>null</code> if no device was found
     */
    public @Nullable CLEnvironment getEnvironment() {
        return this.getEnvironment(CLEnvironmentOptions.DEFAULT);
    }

    /**
     * Retrieves an environment that follows the specified requirements.
     *
     * @param options The requirements for the requested environment
     * @return The environment for a device with those properties or <code>null</code> if no device was found
     */
    public @Nullable CLEnvironment getEnvironment(CLEnvironmentOptions options) {
        for (DeviceInfo deviceInfo : this.getPriorityDevices()) {
            if (options.testDevice(deviceInfo)) {
                CLEnvironment environment = this.getEnvironment(deviceInfo);
                if (environment == null) {
                    continue;
                }
                return environment;
            }
        }
        return null;
    }

    /**
     * Retrieves the environment for the specified device.
     *
     * @param deviceInfo The device to retrieve the environment for
     * @return The environment for that device or <code>null</code> if no device was found or <code>null</code> if no device was found
     */
    public @Nullable CLEnvironment getEnvironment(DeviceInfo deviceInfo) {
        if (this.invalidDevices.contains(deviceInfo)) {
            return null;
        }

        CLEnvironment environment = this.environments.get(deviceInfo);
        if (environment != null) {
            return environment;
        }
        try {
            this.deviceLock.lock();

            // Make sure a different thread didn't add an environment
            environment = this.environments.get(deviceInfo);
            if (environment != null) {
                return environment;
            }

            try {
                environment = CompletableFuture.supplyAsync(() -> {
                    try {
                        return new CLEnvironment(deviceInfo);
                    } catch (CLException e) {
                        throw new CompletionException(e);
                    }
                }, VeilRenderSystem.renderThreadExecutor()).join();
                this.environments.put(deviceInfo, environment);
                return environment;
            } catch (CompletionException e) {
                Throwable t = e.getCause() != null ? e.getCause() : e;
                this.invalidDevices.add(deviceInfo);
                LOGGER.error("Failed to create environment for device: " + deviceInfo.name(), t);
                return null;
            }
        } finally {
            this.deviceLock.unlock();
        }
    }

    /**
     * @return All platforms OpenCL can be run on
     */
    public PlatformInfo[] getPlatforms() {
        return this.platforms;
    }

    /**
     * @return All devices for all platforms sorted by compute priority
     */
    public List<DeviceInfo> getPriorityDevices() {
        return this.priorityDevices;
    }

    @ApiStatus.Internal
    @Override
    public void free() {
        instance.environments.values().forEach(CLEnvironment::free);
        instance.environments.clear();
    }

    /**
     * Attempts to release all OpenCL resources without initializing OpenCL.
     */
    @ApiStatus.Internal
    public static void tryFree() {
        if (instance != null) {
            instance.free();
            instance = null;
        }
    }

    /**
     * @return The static Veil OpenCL implementation
     */
    public static VeilOpenCL get() {
        if (instance == null) {
            instance = new VeilOpenCL();
        }
        return instance;
    }

    /**
     * IntBuffer implementation of {@link #checkCLError(int)}
     */
    public static void checkCLError(IntBuffer errcode) throws CLException {
        checkCLError(errcode.get(0));
    }

    /**
     * Checks if the specified error code was not {@link CL10#CL_SUCCESS}
     *
     * @param errcode The error code to validate
     * @throws CLException If the error code was not success
     */
    public static void checkCLError(int errcode) throws CLException {
        if (errcode != CL_SUCCESS) {
            throw new CLException(errcode);
        }
    }

    /**
     * Retrieves the specified program build info string from the specified program and device.
     *
     * @param program The program to get the build info for
     * @param device  The device the program was built on
     * @param param   The parameter to get
     * @return The program build info string
     * @throws CLException If any error occurred while trying to get the string
     */
    public static String getProgramBuildInfo(long program, long device, int param) throws CLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer log_size = stack.mallocPointer(1);
            VeilOpenCL.checkCLError(clGetProgramBuildInfo(program, device, param, (PointerBuffer) null, log_size));

            ByteBuffer log_data = stack.malloc((int) log_size.get(0));
            VeilOpenCL.checkCLError(clGetProgramBuildInfo(program, device, param, log_data, null));

            return MemoryUtil.memASCII(log_data);
        }
    }

    /**
     * Retrieves the specified string parameter from the specified platform.
     *
     * @param platform The platform to get the string from
     * @param param    The parameter to get
     * @return The platform info string
     * @throws CLException If any error occurred while trying to get the string
     */
    public static String getPlatformInfoString(long platform, int param) throws CLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetPlatformInfo(platform, param, (ByteBuffer) null, pp));
            int bytes = (int) pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetPlatformInfo(platform, param, buffer, null));

            return MemoryUtil.memUTF8(buffer, bytes - 1);
        }
    }

    /**
     * Retrieves the specified string parameter from the specified device.
     *
     * @param device The device to get the string from
     * @param param  The parameter to get
     * @return The device info string
     * @throws CLException If any error occurred while trying to get the string
     */
    public static String getDeviceInfoString(long device, int param) throws CLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            checkCLError(clGetDeviceInfo(device, param, (ByteBuffer) null, pp));
            int bytes = (int) pp.get(0);

            ByteBuffer buffer = stack.malloc(bytes);
            checkCLError(clGetDeviceInfo(device, param, buffer, null));

            return MemoryUtil.memUTF8(buffer, bytes - 1);
        }
    }

    /**
     * Retrieves the specified integer parameter from the specified device.
     *
     * @param device The device to get the int from
     * @param param  The parameter to get
     * @return The device info int
     * @throws CLException If any error occurred while trying to get the int
     */
    public static int getDeviceInfoInt(long device, int param) throws CLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pl = stack.mallocInt(1);
            checkCLError(clGetDeviceInfo(device, param, pl, null));
            return pl.get(0);
        }
    }

    /**
     * Retrieves the specified long parameter from the specified device.
     *
     * @param device The device to get the long from
     * @param param  The parameter to get
     * @return The device info long
     * @throws CLException If any error occurred while trying to get the long
     */
    public static long getDeviceInfoLong(long device, int param) throws CLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pl = stack.mallocLong(1);
            checkCLError(clGetDeviceInfo(device, param, pl, null));
            return pl.get(0);
        }
    }

    private static PlatformInfo[] requestPlatforms() throws CLException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer num_platforms = stack.mallocInt(1);
            checkCLError(clGetPlatformIDs(null, num_platforms));

            if (num_platforms.get(0) == 0) {
                return new PlatformInfo[0];
            }

            PointerBuffer platforms = stack.mallocPointer(num_platforms.get(0));
            checkCLError(clGetPlatformIDs(platforms, (IntBuffer) null));

            PlatformInfo[] platformInfos = new PlatformInfo[platforms.capacity()];
            for (int i = 0; i < platformInfos.length; i++) {
                platformInfos[i] = PlatformInfo.create(platforms.get(i), stack);
            }

            return platformInfos;
        }
    }

    /**
     * Information about a platform that can run OpenCL.
     *
     * @param id           The id of this platform
     * @param capabilities The capabilities supported on this platform
     * @param profile      The profile name supported by the implementation
     * @param version      OpenCL version string
     * @param name         Platform name string
     * @param vendor       Platform vendor string
     * @param devices      The list of devices available on this platform
     * @author Ocelot
     */
    public record PlatformInfo(long id,
                               CLCapabilities capabilities,
                               String profile,
                               String version,
                               String name,
                               String vendor,
                               DeviceInfo[] devices) {

        public static PlatformInfo create(long platform, MemoryStack stack) throws CLException {
            CLCapabilities caps = CL.createPlatformCapabilities(platform);
            String profile = getPlatformInfoString(platform, CL_PLATFORM_PROFILE);
            String version = getPlatformInfoString(platform, CL_PLATFORM_VERSION);
            String name = getPlatformInfoString(platform, CL_PLATFORM_NAME);
            String vendor = getPlatformInfoString(platform, caps.cl_khr_icd ? CL_PLATFORM_ICD_SUFFIX_KHR : CL_PLATFORM_VENDOR);

            IntBuffer num_devices = stack.mallocInt(1);
            checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, null, num_devices));
            DeviceInfo[] deviceInfos = new DeviceInfo[num_devices.get(0)];

            if (deviceInfos.length > 0) {
                PointerBuffer devices = stack.mallocPointer(deviceInfos.length);
                checkCLError(clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, devices, (IntBuffer) null));

                for (int i = 0; i < deviceInfos.length; i++) {
                    deviceInfos[i] = DeviceInfo.create(devices.get(i), caps);
                }
            }

            return new PlatformInfo(platform, caps, profile, version, name, vendor, deviceInfos);
        }
    }

    /**
     * Information about a device on an OpenCL platform.
     *
     * @param platform                 The platform this device is on
     * @param id                       The id of this device
     * @param capabilities             The capabilities of this device
     * @param type                     The OpenCL device type
     * @param vendorId                 A unique device vendor identifier
     * @param maxComputeUnits          The number of parallel compute units on the OpenCL device. A work-group executes on a single compute unit. The minimum value is 1
     * @param maxWorkItemDimensions    Maximum dimensions that specify the global and local work-item IDs used by the data parallel execution model. (Refer to {@link CL10#clEnqueueNDRangeKernel(long, long, int, PointerBuffer, PointerBuffer, PointerBuffer, PointerBuffer, PointerBuffer) clEnqueueNDRangeKernel})
     * @param maxWorkGroupSize         Maximum number of work-items in a work-group that a device is capable of executing on a single compute unit, for any given kernel-instance running on the device
     * @param maxMemAllocSize          Max size of memory object allocation in bytes
     * @param maxClockFrequency        Maximum configured clock frequency of the device in MHz
     * @param addressBits              The default compute device address space size of the global address space specified as an unsigned integer value in bits. Currently supported values are 32 or 64 bits
     * @param available                If this device is able to execute commands sent to it
     * @param compilerAvailable        If this device has a compiler available to compile program source
     * @param requireManualInteropSync If this requires the user to manually sync data when using CL/GL interoperability
     * @param name                     Device name string
     * @param vendor                   Vendor name string
     * @param driverVersion            OpenCL software driver version string. Follows a vendor-specific format
     * @param profile                  The profile name supported by the device
     * @param version                  The OpenCL version supported by the device
     * @param openclCVersion           OpenCL C version string or <code>null</code> if not supported
     * @author Ocelot
     */
    public record DeviceInfo(long platform,
                             long id,
                             CLCapabilities capabilities,
                             long type,
                             int vendorId,
                             int maxComputeUnits,
                             int maxWorkItemDimensions,
                             long maxWorkGroupSize,
                             long maxMemAllocSize,
                             int maxClockFrequency,
                             int addressBits,
                             boolean available,
                             boolean compilerAvailable,
                             boolean requireManualInteropSync,
                             String name,
                             String vendor,
                             String driverVersion,
                             String profile,
                             String version,
                             @Nullable String openclCVersion
    ) {

        public static DeviceInfo create(long device, CLCapabilities platformCapabilities) throws CLException {
            CLCapabilities caps = CL.createDeviceCapabilities(device, platformCapabilities);
            long platform = getDeviceInfoLong(device, CL_DEVICE_PLATFORM);
            long deviceType = getDeviceInfoLong(device, CL_DEVICE_TYPE);
            int vendorId = getDeviceInfoInt(device, CL_DEVICE_VENDOR_ID);
            int maxComputeUnits = getDeviceInfoInt(device, CL_DEVICE_MAX_COMPUTE_UNITS);
            int maxWorkItemDimensions = getDeviceInfoInt(device, CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
            long maxWorkGroupSize = getDeviceInfoLong(device, CL_DEVICE_MAX_WORK_GROUP_SIZE);
            long maxMemAllocSize = getDeviceInfoLong(device, CL_DEVICE_MAX_MEM_ALLOC_SIZE);
            int maxClockFrequency = getDeviceInfoInt(device, CL_DEVICE_MAX_CLOCK_FREQUENCY);
            int addressBits = getDeviceInfoInt(device, CL_DEVICE_ADDRESS_BITS);
            boolean available = getDeviceInfoInt(device, CL_DEVICE_AVAILABLE) == CL_TRUE;
            boolean compilerAvailable = getDeviceInfoInt(device, CL_DEVICE_COMPILER_AVAILABLE) == CL_TRUE;
            boolean requireManualInteropSync = !caps.OpenCL12 || getDeviceInfoInt(device, CL_DEVICE_PREFERRED_INTEROP_USER_SYNC) == CL_TRUE;

            String name = getDeviceInfoString(device, CL_DEVICE_NAME);
            String vendor = getDeviceInfoString(device, CL_DEVICE_VENDOR);
            String driverVersion = getDeviceInfoString(device, CL_DRIVER_VERSION);
            String profile = getDeviceInfoString(device, CL_DEVICE_PROFILE);
            String version = getDeviceInfoString(device, CL_DEVICE_VERSION);
            String openclCVersion = caps.OpenCL11 ? getDeviceInfoString(device, CL_DEVICE_OPENCL_C_VERSION) : null;
            return new DeviceInfo(platform, device, caps, deviceType, vendorId, maxComputeUnits, maxWorkItemDimensions, maxWorkGroupSize, maxMemAllocSize, maxClockFrequency, addressBits, available, compilerAvailable, requireManualInteropSync, name, vendor, driverVersion, profile, version, openclCVersion);
        }

        /**
         * An OpenCL device that is the host processor. The
         * host processor runs the OpenCL implementations
         * and is a single or multi-core CPU
         */
        public boolean isCpu() {
            return (this.type & CL_DEVICE_TYPE_CPU) > 0;
        }

        /**
         * An OpenCL device that is a GPU. By this we mean
         * that the device can also be used to accelerate a 3D
         * API such as OpenGL or DirectX
         */
        public boolean isGpu() {
            return (this.type & CL_DEVICE_TYPE_GPU) > 0;
        }

        /**
         * Dedicated OpenCL accelerators (for example the
         * IBM CELL Blade). These devices communicate
         * with the host processor using a peripheral
         * interconnect such as PCIe.
         */
        public boolean isAccelerator() {
            return (this.type & CL_DEVICE_TYPE_ACCELERATOR) > 0;
        }

        /**
         * The default OpenCL device in the system. The
         * default device cannot be a
         * CL_DEVICE_TYPE_CUSTOM device.
         */
        public boolean isDefault() {
            return (this.type & CL_DEVICE_TYPE_DEFAULT) > 0;
        }
    }
}
