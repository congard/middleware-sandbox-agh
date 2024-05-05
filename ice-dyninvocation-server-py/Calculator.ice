module DynamicInvocation {
    struct Complex {
        double re;
        double im;
    };

    struct Stats {
        double min;
        double max;
        double avg;
        double med;
    };

    sequence<double> DoubleArray;
    sequence<Complex> ComplexArray;

    exception TooFewArgumentException {};

    interface Calculator {
        idempotent double muld(double p1, double p2);
        idempotent double mmuld(DoubleArray d) throws TooFewArgumentException;

        idempotent Stats stat(DoubleArray d) throws TooFewArgumentException;

        idempotent Complex mulc(Complex c1, Complex c2);
        idempotent Complex mmulc(ComplexArray c) throws TooFewArgumentException;
    };
};
