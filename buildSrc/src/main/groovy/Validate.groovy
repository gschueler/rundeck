
interface Validate{
    void log(String desc)
    void ok(String desc)
    void warn(String desc)
    boolean fail(String desc)
    boolean require(String desc,def val)
    boolean require(String desc,def val, boolean affirm)
    boolean expect(String desc,def val)
    boolean isValid()
}
