#define BLOCK_SOLID VEIL_MAYBE_USE_HDR(0)
#define BLOCK_CUTOUT VEIL_MAYBE_USE_HDR(1)
#define BLOCK_CUTOUT_MIPPED VEIL_MAYBE_USE_HDR(2)
#define BLOCK_TRANSLUCENT VEIL_MAYBE_USE_HDR(3)

#define ENTITY_SOLID VEIL_MAYBE_USE_HDR(4)
#define ENTITY_CUTOUT VEIL_MAYBE_USE_HDR(5)
#define ENTITY_TRANSLUCENT VEIL_MAYBE_USE_HDR(6)
#define ENTITY_TRANSLUCENT_EMISSIVE VEIL_MAYBE_USE_HDR(7)

#define PARTICLE VEIL_MAYBE_USE_HDR(8)
#define ARMOR_CUTOUT VEIL_MAYBE_USE_HDR(9)
#define LEAD 10
#define BREAKING 11
#define CLOUD 12
#define WORLD_BORDER 13

#define MATERIAL_BITMASK 0x7Fu
#define MATERIAL_HDR_FLAG 0x80u

#ifdef VEIL_USE_HDR
    #define VEIL_MAYBE_USE_HDR(material) (material | MATERIAL_HDR_FLAG)
#else
    #define VEIL_MAYBE_USE_HDR(material) material
#endif

bool isBlock(uint material) {
    material &= MATERIAL_BITMASK;
    return material >= BLOCK_SOLID && material <= BLOCK_TRANSLUCENT;
}

bool isEntity(uint material) {
    material &= MATERIAL_BITMASK;
    return material >= ENTITY_SOLID && material <= ENTITY_TRANSLUCENT_EMISSIVE;
}

bool isEmissive(uint material) {
    material &= MATERIAL_BITMASK;
    return material == ENTITY_TRANSLUCENT_EMISSIVE;
}

bool usesHDR(uint material) {
    return bool(material & MATERIAL_HDR_FLAG);
}
