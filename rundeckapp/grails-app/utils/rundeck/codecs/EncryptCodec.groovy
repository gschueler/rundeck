package rundeck.codecs

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Base64
import org.jasypt.encryption.pbe.PBEByteEncryptor
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor
import org.jasypt.encryption.pbe.config.EnvironmentStringPBEConfig
import org.jasypt.registry.AlgorithmRegistry

import java.security.Security
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Created by greg on 7/12/16.
 */
class EncryptCodec {
    static {
        Security.addProvider new BouncyCastleProvider()
    }
    static private final Object synch = new Object()
    static private ConcurrentMap<String, PBEByteEncryptor> encryptorMap = new ConcurrentHashMap<>()
    static private PBEByteEncryptor encryptor
    static class ConsoleEnvPBEConfig extends EnvironmentStringPBEConfig {
        String configName
        boolean passwordConsole

        @Override
        char[] getPasswordCharArray() {
            if (passwordConsole) {
                Console console = System.console()
                if (!console) {
                    throw new IllegalArgumentException("No system console available.")
                }
                return console.readPassword("Enter master password for [${configName}]: ")
            }
            return super.getPasswordCharArray()
        }
    }

    static configViaSysAndEnvProperty(ConsoleEnvPBEConfig config, String prop, String envProp) {

        configViaEnvProperty(config, prop, envProp)
        configViaSysProperty(config, prop, envProp)

    }

    static configViaEnvProperty(ConsoleEnvPBEConfig config, String prop, String envProp) {
        def env = "RD_ENCRYPTION_${envProp.replaceAll('\\.', '_')}".toUpperCase()
        if (System.getenv(env)) {
            config["${prop}EnvName"] = env
        }
    }

    static configViaSysProperty(ConsoleEnvPBEConfig config, String prop, String envProp) {

        def propName = "rd.encryption.${envProp}"
        if (System.getProperty(propName)) {
            config["${prop}SysPropertyName"] = propName
        }
    }

    private static PBEByteEncryptor load(String configName) {

        def encryptor = encryptorMap.get(configName)
        if (null == encryptor) {
            encryptor = new StandardPBEByteEncryptor()
            //prompt
            ConsoleEnvPBEConfig config = new ConsoleEnvPBEConfig(configName: configName);
//            println AlgorithmRegistry.getAllPBEAlgorithms().grep{it==~/^.*BC$/}
            //default settings
            config.setAlgorithm("PBEWithMD5AndDES");
            config.setAlgorithm("PBEWITHSHA256AND256BITAES-CBC-BC");
//                config.setAlgorithm("PBEWithMD5AndTripleDES");
            config.providerName = 'BC'
//            config.providerClassName = 'org.bouncycastle.jce.provider.BouncyCastleProvider'

            config.keyObtentionIterations = 1000

            configViaSysAndEnvProperty(config, "Algorithm", "${configName}.algorithm")
            configViaSysAndEnvProperty(config, "ProviderName", "${configName}.provider")
            configViaSysAndEnvProperty(config, "ProviderNameClassName", "${configName}.providerClass")
            configViaSysAndEnvProperty(config, "KeyObtentionIterations", "${configName}.keyObtentions")
            configViaSysAndEnvProperty(config, "Password", "${configName}.password")

            //trigger console to prompt user for password if not already set.
            config.passwordConsole = true

            encryptor.config = config
            encryptorMap.putIfAbsent(configName, encryptor)
        }
        return encryptor
    }
    def decode = { string ->
        decrypt(string)
    }

    def encode = { string ->
        encrypt(string)
    }

    static def decrypt(string, String configName = 'default') {

        def decrypt = load(configName).decrypt(Base64.decode(string.toString()))
        return new String(decrypt)
    }

    static def encrypt(string, String configName = 'default') {

        byte[] crypt = load(configName).encrypt(string.toString().getBytes())
        return Base64.toBase64String(crypt)
    }

    static def main(String[] args) {
        if (args == null || args.length < 1 || !(args[0] in ['encrypt', 'decrypt'])) {
            System.err.println("Usage: encrypt/decrypt [config] [value]")
            return
        }
        def action = args[0]
        def configName = 'default'
        if (args.length > 2) {
            configName = args[1]
        }


        String value

        if (args.length > 2) {
            value = args[2]
        } else if (args.length > 1) {
            value = args[1]
        } else if (args.length < 2) {
            //read input
            def prompt = 'Enter value to %1s for [%2s]: '
            def pargs = [action, configName]
            def console = System.console()
            char[] result = action == 'encrypt' ? console.readPassword(prompt, *pargs) : console.readLine(prompt, *pargs)
            value = new String(result)
        }

        switch (args[0]) {
            case 'encrypt':
                println encrypt(value, configName)
                break;
            case 'decrypt':
                println decrypt(value, configName)
                break;
        }
    }
}
