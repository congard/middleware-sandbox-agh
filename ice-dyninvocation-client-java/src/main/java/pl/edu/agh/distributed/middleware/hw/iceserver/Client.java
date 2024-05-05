package pl.edu.agh.distributed.middleware.hw.iceserver;

import com.zeroc.Ice.*;

import java.io.Closeable;
import java.io.IOException;
import java.lang.Object;
import java.util.function.Consumer;
import java.util.function.Function;

public class Client implements Closeable {
    private final Communicator communicator;
    private final com.zeroc.Ice.ObjectPrx base;

    private Client(String[] args) {
        communicator = com.zeroc.Ice.Util.initialize(args);
        base = communicator.stringToProxy("Calculator:default -p 10000");
    }

    public static void main(String[] args) {
        try (var client = new Client(args)) {
            println(client.muld(2, 3));
            println(client.mmuld(2, 3, 4));
            // println(client.mmuld(2)); // exception

            println(client.stat(2, 3, 4, 5, 6, 10));

            println(client.mulc(new Complex(3, 2), new Complex(4, -1)));

            println(client.mmulc(
                new Complex(3, 2),
                new Complex(4, -1),
                new Complex(14, -5)
            ));
        } catch (IOException | UserException e) {
            throw new RuntimeException(e);
        }
    }

    private static void println(Object o) {
        System.out.println(o);
    }

    private <T> T dynamicInvocation(
        String funName,
        Consumer<OutputStream> paramWriter,
        Function<InputStream, T> reader
    ) throws UserException {
        var out = new OutputStream(communicator);
        out.startEncapsulation();
        paramWriter.accept(out);
        out.endEncapsulation();

        byte[] inParams = out.finished();
        var r = base.ice_invoke(funName, OperationMode.Idempotent, inParams);

        var in = new InputStream(communicator, r.outParams);

        if (r.returnValue) {
            in.startEncapsulation();
            var result = reader.apply(in);
            in.endEncapsulation();
            return result;
        } else {
            in.startEncapsulation();
            in.throwException();
        }

        return null;
    }

    private Double muld(String funName, Consumer<OutputStream> paramWriter) throws UserException {
        return dynamicInvocation(funName, paramWriter, InputStream::readDouble);
    }

    private double muld(double d1, double d2) throws UserException {
        return muld("muld", out -> {
            out.writeDouble(d1);
            out.writeDouble(d2);
        });
    }

    private double mmuld(double... d) throws UserException {
        return muld("mmuld", out -> out.writeDoubleSeq(d));
    }

    private Stats stat(double... d) throws UserException {
        return dynamicInvocation("stat",
                out -> out.writeDoubleSeq(d),
                in -> new Stats(in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble()));
    }

    private Complex mulc(String funName, Consumer<OutputStream> paramWriter) throws UserException {
        return dynamicInvocation(funName, paramWriter, in ->
                new Complex(in.readDouble(), in.readDouble()));
    }

    private Complex mulc(Complex c1, Complex c2) throws UserException {
        return mulc("mulc", out -> {
            out.writeDouble(c1.re);
            out.writeDouble(c1.im);
            out.writeDouble(c2.re);
            out.writeDouble(c2.im);
        });
    }

    private Complex mmulc(Complex... complexes) throws UserException {
        return mulc("mmulc", out -> {
            out.writeSize(complexes.length);

            for (var c : complexes) {
                out.writeDouble(c.re);
                out.writeDouble(c.im);
            }
        });
    }

    @Override
    public void close() throws IOException {
        communicator.close();
    }

    private record Stats(double min, double max, double avg, double med) {}

    private record Complex(double re, double im) {}
}