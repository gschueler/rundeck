import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

interface Check{
    def init(String val)
    def update(java.util.zip.ZipFile zip,java.util.zip.ZipEntry entry)
    def update(String name)
    boolean validate(String msg, Validate validator)
}
class Base implements Check{
    @Override
    def init(final String val) {
        return null
    }

    @Override
    def update(final ZipFile zip, final ZipEntry entry) {
        return update(entry.name)
    }

    @Override
    def update(final String path) {
        return null
    }

    @Override
    boolean validate(final String msg, final Validate validator) {
        return true
    }
}
class MustExist extends Base{
    String val
    boolean exists=false
    boolean affirm=false

    MustExist(final String val) {
        this.val = val
    }

    MustExist(final String val, final boolean affirm) {
        this.val = val
        this.affirm = affirm
    }

    @Override
    def init(final String val) {
        this.val=val
    }

    @Override
    def update(final String path) {
        if(path==val){
            exists=true
        }
    }

    @Override
    boolean validate(final String msg, final Validate validator) {
        validator.require("[$msg] ${val} MUST exist. Result: (${exists ?: false})", exists, affirm)
    }
}
class Match extends Base{
    String pattern
    boolean match
    List<String> matched=[]

    Match(final String pattern) {
        this(pattern, true)
    }

    Match(final String pattern, final boolean match) {
        this.pattern = pattern
        this.match = match
    }

    @Override
    def update(final String path) {
        if(path=~/$pattern/){
            matched<<path
        }
    }

    @Override
    boolean validate(final String msg, final Validate validator) {
        validator.require(
            "[${msg}] ~/${pattern}/ SHOULD${!match?' NOT':''} match. Result: ${matched}",
            match && matched || !match && !matched
        )
    }
}
class Count extends Base{
    String base
    int min=-1
    int max=-1
    int counted
    List<String> expected=null
    List<String> unexpected=[]

    Count(final String base, final List<String> expected) {
        this.base = base
        this.expected = expected
        this.min=expected.size()
        this.max=expected.size()
    }

    Count(final String base, final int min) {
        this.base = base
        this.min = min
    }

    Count(final String base, final int min, final int max) {
        this.base = base
        this.min = min
        this.max = max
    }

    @Override
    def update(final String path) {
        if(path.startsWith(base)){
            counted++
            if (expected != null && expected.contains(path)) {
                expected.remove(path)
            }else if(expected!=null){
                unexpected<<path
            }
        }
    }

    @Override
    boolean validate(final String msg, final Validate v) {
        boolean valid=false
        if(min>=0 && counted<min){
            valid=false
        }
        if(max>=0 && counted>max){
            valid=false
        }
        if (expected != null && expected.size()>0) {
            v.fail(
                "[${msg}] \"${base}\" Expected files were not found: ${expected}"
            )
        }
        if (expected != null && unexpected.size()>0) {
            v.fail(
                "[${msg}] \"${base}\" Unexpected contents encountered: ${unexpected}"
            )
        }
        if (min==max) {
            v.ok("[${msg}] \"${base}\" MUST have == ${min} files. Found: ${counted}")
            return v.require(
                "[${msg}] \"${base}\" MUST have == ${min} files. Result: ${counted}",
                min==counted
            )
        } else if (max>=0 && min>=0) {
            return v.require(
                "[${msg}] \"${base}\" SHOULD have ${min} <= X <= ${max} files. Result: ${counted}",
                counted <= max && counted >= min
            )
        } else if (min>>0 && max<0) {
            //at least
            return v.require(
                "[${msg}] \"${base}\" MUST have >=${min} files. Result: ${counted}",
                counted >= min
            )
        } else if (max>=0 && min<0) {
            return v.require(
                "[${msg}] \"${base}\" SHOULD have <= ${max}files. Result: ${counted}",
                counted <= max
            )
        }
        valid
    }
}
class SHASum extends MustExist{
    File compare
    String fileSha
    String entrySha

    SHASum(final String val, final File compare) {
        super(val)
        this.compare = compare

        compare.withInputStream{
            fileSha=getSha256(it)
        }
    }

    @Override
    def update(final ZipFile zip, final ZipEntry entry) {
        super.update(entry.name)
        if(entry.name==val) {
            entrySha = getSha256(zip.getInputStream(entry))
        }
    }

    @Override
    boolean validate(final String msg, final Validate validator) {
        return super.validate(msg, validator) &&
               validator.require(
                   "[${msg}] \"${val}\" SHA-256 MUST match \"${compare}\". Seen: ($entrySha) Expected: (${fileSha})",
                   entrySha == fileSha
               )
    }
    static String getSha256(InputStream fis){
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");

        byte[] dataBytes = new byte[10240];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        };
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
        }
        hexString.toString()
    }
}
class Stem extends Base{
    List<String> ignored =[]
    String prefix
    Map<String,List<String>> stems=[:]
    Pattern extpat = Pattern.compile(/^(.+)\.([a-z]+)$/)
    Pattern checkpat = Pattern.compile(/^(([a-zA-Z0-9._-]+)+)-(v?\d+(\.\d+)*)(.*?)$/)
    def init(String val){
        prefix=val
    }
    def update(String path){
        if(!path.startsWith(prefix)){
            return
        }
        String subpath = path.substring(prefix.length())
        java.util.regex.Matcher m1 = extpat.matcher(subpath)
        if(!m1.matches()){
            return
        }
        def basename=m1.group(1)
        def fileext=m1.group(2)
        if(!basename || !fileext){
            return
        }
        java.util.regex.Matcher m = checkpat.matcher(basename)
        if(!m.matches()) {
            return
        }
        def stem=m.group(1)
        def key=stem+':'+fileext
        if(!ignored.contains(key)) {
            stems.putIfAbsent(key, [])
            stems[key] << subpath
        }
    }
    boolean validate(String msg, Validate v){
        boolean valid=true
        stems.keySet().each{k->
            valid&=v.require("[$msg] stem \"${prefix}(${k})\" MUST be unique: ${stems[k].size()<2}: ${stems[k]}", stems[k].size()<2)
        }
        valid
    }
}
