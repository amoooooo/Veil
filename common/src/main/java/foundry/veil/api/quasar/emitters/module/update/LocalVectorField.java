package foundry.veil.api.quasar.emitters.module.update;//package foundry.veil.quasar.emitters.modules.particle.update.fields;
//
//import foundry.veil.util.FastNoiseLite;
//import net.minecraft.world.phys.Vec3;
//
//import java.util.function.BiFunction;
//import java.util.function.Function;
//
/// **
// * Vector Field with a set range around a world position.
// * @see foundry.veil.quasar.emitters.modules.particle.update.fields.VectorField
// */
//public class LocalVectorField extends VectorField {
//    private final Vec3 position;
//    private final float range;
//    private final Shape shape;
//    public LocalVectorField(FastNoiseLite noise, float strength, Function<Vec3, Vec3> vectorFunction, Vec3 position, float range, Shape shape) {
//        super(noise, strength, vectorFunction);
//        this.position = position;
//        this.range = range;
//        this.shape = shape;
//    }
//
//    @Override
//    public Vec3 getVector(Vec3 position) {
//        if (shape.inShape(position, this)) {
//            return super.getVector(position);
//        } else {
//            return Vec3.ZERO;
//        }
//    }
//
//    public enum Shape {
//        SPHERE((position, field) -> {
//            return position.distanceTo(field.position) <= field.range;
//        }),
//        CUBE((position, field) -> {
//            return Math.abs(position.x - field.position.x) <= field.range &&
//                    Math.abs(position.y - field.position.y) <= field.range &&
//                    Math.abs(position.z - field.position.z) <= field.range;
//        }),
//        CYLINDER((position, field) -> {
//            return Math.abs(position.x - field.position.x) <= field.range &&
//                    Math.abs(position.z - field.position.z) <= field.range;
//        }),
//        CONE((position, field) -> {
//            return Math.abs(position.x - field.position.x) <= field.range &&
//                    Math.abs(position.z - field.position.z) <= field.range &&
//                    Math.abs(position.y - field.position.y) <= field.range;
//        }),
//        TORUS((position, field) -> {
//            // check if the position is in the torus, with the center of the torus at field.position
//            double x = position.x - field.position.x;
//            double y = position.y - field.position.y;
//            double z = position.z - field.position.z;
//            double range = field.range;
//            double r = Math.sqrt(x*x + z*z);
//            return Math.abs(y) <= range &&
//                    Math.abs(r - range) <= range;
//        }),
//        PYRAMID((position, field) -> {
//            // check if the position is in the pyramid, of height 2*range, with the base centered at field.position
//            // the base is a square with side length 2*range
//            // the top is at field.position.y + 2*range
//            double x = position.x - field.position.x;
//            double y = position.y - field.position.y;
//            double z = position.z - field.position.z;
//            double range = field.range;
//            return Math.abs(x) <= range &&
//                    Math.abs(z) <= range &&
//                    y <= 2*range - Math.sqrt(x*x + z*z);
//        }),
//        PLANE((position, field) -> {
//            return Math.abs(position.x - field.position.x) <= field.range &&
//                    Math.abs(position.z - field.position.z) <= field.range;
//        })
//        ;
//
//        // shapes that are centered at the position, with a function that takes a vec3 and a LocalVectorField and returns a boolean if the vec3 is in the shape
//
//        BiFunction<Vec3, LocalVectorField, Boolean> shapeFunction;
//
//        Shape(BiFunction<Vec3, LocalVectorField, Boolean> shapeFunction) {
//            this.shapeFunction = shapeFunction;
//        }
//
//        public boolean inShape(Vec3 position, LocalVectorField field) {
//            return shapeFunction.apply(position, field);
//        }
//    }
//}
