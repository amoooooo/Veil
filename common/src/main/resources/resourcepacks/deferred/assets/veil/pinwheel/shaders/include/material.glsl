#define BLOCK_SOLID 0
#define BLOCK_CUTOUT 1
#define BLOCK_CUTOUT_MIPPED 2
#define BLOCK_TRANSLUCENT 3

#define ENTITY_SOLID 4
#define ENTITY_CUTOUT 5
#define ENTITY_TRANSLUCENT 6
#define ENTITY_TRANSLUCENT_EMISSIVE 7

#define PARTICLE 8
#define ARMOR_CUTOUT 9
#define LEAD 10
#define BREAKING 11
#define CLOUD 12
#define WORLD_BORDER 13

// For the transparent pipeline, don't override HDR scale with 0
#if defined(VEIL_HDR_SCALE) && VEIL_HDR_SCALE > 0.0
    #define VEIL_TRANSPARENT_USE_DEFINED_HDR_SCALE() fragHDRScale = vec4(VEIL_HDR_SCALE, 0.0, 0.0, 1.0)
#else
    #define VEIL_HDR_SCALE 0.0
    #define VEIL_TRANSPARENT_USE_DEFINED_HDR_SCALE()
#endif
#define VEIL_OPAQUE_USE_DEFINED_HDR_SCALE() fragHDRScale = vec4(VEIL_HDR_SCALE, 0.0, 0.0, 1.0)

bool isBlock(uint material) {
    return material >= BLOCK_SOLID && material <= BLOCK_TRANSLUCENT;
}

bool isEntity(uint material) {
    return material >= ENTITY_SOLID && material <= ENTITY_TRANSLUCENT_EMISSIVE;
}

bool isEmissive(uint material) {
    return material == ENTITY_TRANSLUCENT_EMISSIVE;
}
