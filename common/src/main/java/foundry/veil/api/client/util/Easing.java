package foundry.veil.api.client.util;

import net.minecraft.util.Mth;

/**
 * @author amo
 */
public enum Easing {
    LINEAR {
        public float ease(float x) {
            return x;
        }
    },
    EASE_IN_QUAD {
        public float ease(float x) {
            return x * x;
        }
    },
    EASE_OUT_QUAD {
        public float ease(float x) {
            return 1 - (1 - x) * (1 - x);
        }
    },
    EASE_IN_OUT_QUAD {
        public float ease(float x) {
            return x < 0.5 ? 2 * x * x : (float) (1 - Math.pow(-2 * x + 2, 2) / 2);
        }
    },
    EASE_IN_CUBIC {
        public float ease(float x) {
            return x * x * x;
        }
    },
    EASE_OUT_CUBIC {
        public float ease(float x) {
            return (float) (1 - Math.pow(1 - x, 3));
        }
    },
    EASE_IN_OUT_CUBIC {
        public float ease(float x) {
            return x < 0.5 ? 4 * x * x * x : (float) (1 - Math.pow(-2 * x + 2, 3) / 2);
        }
    },
    EASE_IN_QUART {
        public float ease(float x) {
            return x * x * x * x;
        }
    },
    EASE_OUT_QUART {
        public float ease(float x) {
            return (float) (1 - Math.pow(1 - x, 4));
        }
    },
    EASE_IN_OUT_QUART {
        public float ease(float x) {
            return x < 0.5 ? 8 * x * x * x * x : (float) (1 - Math.pow(-2 * x + 2, 4) / 2);
        }
    },
    EASE_IN_QUINT {
        public float ease(float x) {
            return x * x * x * x * x;
        }
    },
    EASE_OUT_QUINT {
        public float ease(float x) {
            return (float) (1 - Math.pow(1 - x, 5));
        }
    },
    EASE_IN_OUT_QUINT {
        public float ease(float x) {
            return x < 0.5 ? 16 * x * x * x * x * x : (float) (1 - Math.pow(-2 * x + 2, 5) / 2);
        }
    },
    EASE_IN_SINE {
        public float ease(float x) {
            return 1 - Mth.cos((float) ((x * Math.PI) / 2));
        }
    },
    EASE_OUT_SINE {
        public float ease(float x) {
            return Mth.sin((float) ((x * Math.PI) / 2));
        }
    },
    EASE_IN_OUT_SINE {
        public float ease(float x) {
            return -(Mth.cos((float) (Math.PI * x)) - 1) / 2;
        }
    },
    EASE_IN_EXPO {
        public float ease(float x) {
            return x == 0 ? 0 : (float) Math.pow(2, 10 * x - 10);
        }
    },
    EASE_OUT_EXPO {
        public float ease(float x) {
            return x == 1 ? 1 : (float) (1 - Math.pow(2, -10 * x));
        }
    },
    EASE_IN_OUT_EXPO {
        public float ease(float x) {
            return x == 0
                    ? 0
                    : (float) (x == 1
                    ? 1
                    : x < 0.5
                    ? Math.pow(2, 20 * x - 10) / 2
                    : (2 - Math.pow(2, -20 * x + 10)) / 2);
        }
    },
    EASE_IN_CIRC {
        public float ease(float x) {
            return (float) (1 - Math.sqrt(1 - Math.pow(x, 2)));
        }
    },
    EASE_OUT_CIRC {
        public float ease(float x) {
            return (float) Math.sqrt(1 - Math.pow(x - 1, 2));
        }
    },
    EASE_IN_OUT_CIRC {
        public float ease(float x) {
            return (float) (x < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2);
        }
    },
    EASE_IN_BACK {
        public float ease(float x) {
            return 2.70158F * x * x * x - 1.70158F * x * x;
        }
    },
    EASE_OUT_BACK {
        public float ease(float x) {
            return (float) (1 + 2.70158F * Math.pow(x - 1, 3) + 1.70158F * Math.pow(x - 1, 2));
        }
    },
    EASE_IN_OUT_BACK {
        public float ease(float x) {
            return (float) (x < 0.5
                    ? (Math.pow(2 * x, 2) * ((2.5949095F + 1) * 2 * x - 2.5949095F)) / 2
                    : (Math.pow(2 * x - 2, 2) * ((2.5949095F + 1) * (x * 2 - 2) + 2.5949095F) + 2) / 2);
        }
    },
    EASE_IN_ELASTIC {
        public float ease(float x) {
            return x == 0
                    ? 0
                    : (float) (x == 1
                    ? 1
                    : -Math.pow(2, 10 * x - 10) * Mth.sin((float) ((x * 10 - 10.75) * ((2 * Math.PI) / 3))));
        }
    },
    EASE_OUT_ELASTIC {
        public float ease(float x) {
            return x == 0
                    ? 0
                    : (float) (x == 1
                    ? 1
                    : Math.pow(2, -10 * x) * Mth.sin((float) ((x * 10 - 0.75) * ((2 * Math.PI) / 3))) + 1);
        }
    },
    EASE_IN_OUT_ELASTIC {
        public float ease(float x) {
            return x == 0
                    ? 0
                    : (float) (x == 1
                    ? 1
                    : x < 0.5
                    ? -(Math.pow(2, 20 * x - 10) * Mth.sin((float) ((20 * x - 11.125) * ((2 * Math.PI) / 4.5)))) / 2
                    : (Math.pow(2, -20 * x + 10) * Mth.sin((float) ((20 * x - 11.125) * ((2 * Math.PI) / 4.5)))) / 2 + 1);
        }
    },
    EASE_IN_BOUNCE {
        public float ease(float x) {
            return 1 - bounceOut(1 - x);
        }
    },
    EASE_OUT_BOUNCE {
        public float ease(float x) {
            return 1 - bounceOut(1 - x);
        }
    },
    EASE_IN_OUT_BOUNCE {
        public float ease(float x) {
            return x < 0.5
                    ? (1 - bounceOut(1 - 2 * x)) / 2
                    : (1 + bounceOut(2 * x - 1)) / 2;
        }
    };

    /**
     * Applies the easing to the specified input.
     *
     * @param x The input percentage from <code>0</code> to <code>1</code>
     * @return A remapped value from <code>0</code> to <code>1</code> that follows this easing
     */
    public abstract float ease(float x);

    private static float bounceOut(float x) {
        float n1 = 7.5625F;
        float d1 = 2.75F;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5F / d1) * x + 0.75F;
        } else if (x < 2.5 / d1) {
            return n1 * (x -= 2.25F / d1) * x + 0.9375F;
        } else {
            return n1 * (x -= 2.625F / d1) * x + 0.984375F;
        }
    }
}
