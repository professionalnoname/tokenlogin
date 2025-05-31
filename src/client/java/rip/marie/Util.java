package rip.marie;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Just a few utils
 *
 * @author marie
 */
public class Util
{
    /**
     * Current login status (mostly for debugging)
     */
    public static String status = "*no log yet*";

    /**
     * Unsafe instance used to set the <code>session</code> field
     */
    public static final Unsafe UNSAFE;

    static
    {
        Unsafe temp = null;

        try
        {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe"); // get field

            theUnsafe.setAccessible(true); // make accessible (it's private)

            temp = (Unsafe) theUnsafe.get(null); // get field (null = static)
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        UNSAFE = temp;
    }

    /**
     * Sets the minecraft session to contain the tokens account
     * @param addressField Text input field containing the token
     */
    public static void login(final TextFieldWidget addressField)
    {
        status = "Started login";

        new Thread(() -> { // to prevent the main thread (current screen etc.) from lagging
            try
            {
                final String token = addressField.getText();
                final String[] output = getProfileInfo(token); // [name, uuid]

                setSession(
                        new Session(
                                output[0], // name
                                uuidFromString(output[1]), // uuid
                                token, // accessToken
                                Optional.empty(), // xuid, not needed
                                Optional.empty(), // clientId, not needed
                                Session.AccountType.MSA // microsoft login
                        )
                );

                status = String.format("Logged in as %s", output[0]);
            }
            catch (Throwable t)
            {
                status = t.getMessage();
            }
        }).start();
    }

    /**
     * Turns a hexadecimal string (that belongs to a UUID) into a UUID object
     * @param hex Hexadecimal string
     * @return UUID make from the input string
     * @see java.util.UUID
     */
    private static UUID uuidFromString(final String hex) {
        return new UUID(
                new BigInteger(
                        hex.substring(0, 16),
                        16
                ).longValue(),
                new BigInteger(
                        hex.substring(16),
                        16
                ).longValue()
        );
    }

    /**
     * Fairly aggressive but works!
     * @param session The session to apply to the currently running minecraft session
     * @throws Throwable Incase something goes wrong (instance == null)
     * @see net.minecraft.client.MinecraftClient
     * @see net.minecraft.client.session.Session
     * @see sun.misc.Unsafe#putObject(Object, long, Object)
     * @see sun.misc.Unsafe#objectFieldOffset(Field)
     */
    private static void setSession(final Session session) throws Throwable
    {
        UNSAFE.putObject( // put object into address using raw memory
                MinecraftClient.getInstance(), // minecraft client instance
                UNSAFE.objectFieldOffset( // get field memory address
                        MinecraftClient.class.getDeclaredField("session") // session field
                ),
                session // new session
        );
    }

    /**
     * Gets the username and UUID from the token using the Mojang API
     * @param token Minecraft access token
     * @return String array following this format: [username, uuid as hexadecimal string]
     * @throws Throwable Incase the request fails (e.g. not connected to Wi-Fi)
     */
    public static String[] getProfileInfo(final String token) throws Throwable
    {
        try (final CloseableHttpClient client = HttpClients.createDefault())
        {
            // https://minecraft.wiki/w/Mojang_API
            final HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            request.setHeader("Authorization", "Bearer " + token);

            final JsonObject jsonObject = JsonParser.parseString(
                    EntityUtils.toString(
                            client.execute(request).getEntity(),
                            StandardCharsets.UTF_8)
            ).getAsJsonObject();

            return new String[]{
                    jsonObject.get("name").getAsString(),
                    jsonObject.get("id").getAsString()
            };
        }
    }
}
