package dev.jackraidenph.libraomni.experimental;

import org.slf4j.helpers.MarkerIgnoringBase;

import javax.annotation.processing.Messager;
import java.util.Arrays;

public class MessagerLogger extends MarkerIgnoringBase {

    private final Class<?> owner;
    private final Messager messager;

    public MessagerLogger(Class<?> owner, Messager messager) {
        this.owner = owner;
        this.messager = messager;
    }

    @Override
    public String getName() {
        return owner.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String s) {

    }

    @Override
    public void trace(String s, Object o) {

    }

    @Override
    public void trace(String s, Object o, Object o1) {

    }

    @Override
    public void trace(String s, Object... objects) {

    }

    @Override
    public void trace(String s, Throwable throwable) {

    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public void debug(String s) {
    }

    @Override
    public void debug(String s, Object o) {

    }

    @Override
    public void debug(String s, Object o, Object o1) {

    }

    @Override
    public void debug(String s, Object... objects) {

    }

    @Override
    public void debug(String s, Throwable throwable) {

    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String s) {
        messager.printNote(s);
    }

    @Override
    public void info(String s, Object o) {
        info(s, new Object[]{o});
    }

    @Override
    public void info(String s, Object o, Object o1) {
        info(s, new Object[]{o, o1});
    }

    @Override
    public void info(String s, Object... objects) {
        info(format(s, objects));
    }

    @Override
    public void info(String s, Throwable throwable) {

    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String s) {
        messager.printWarning(s);
    }

    @Override
    public void warn(String s, Object o) {
        warn(s, new Object[]{o});
    }

    @Override
    public void warn(String s, Object... objects) {
        warn(format(s, objects));
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        warn(s, new Object[]{o, o1});
    }

    @Override
    public void warn(String s, Throwable throwable) {

    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String s) {
        messager.printError(s);
    }

    @Override
    public void error(String s, Object o) {
        error(s, new Object[]{o});
    }

    @Override
    public void error(String s, Object o, Object o1) {
        error(s, new Object[]{o, o1});
    }

    @Override
    public void error(String s, Object... objects) {
        error(format(s, objects));
    }

    @Override
    public void error(String s, Throwable throwable) {

    }

    private static String format(String str, Object... args) {
        Object[] strArgs = Arrays.stream(args).map(String::valueOf).toArray();
        return str.replace("{}", "%s").formatted(strArgs);
    }
}
