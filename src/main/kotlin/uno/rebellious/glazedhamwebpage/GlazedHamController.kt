package uno.rebellious.glazedhamwebpage

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView
import java.io.File
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Logger

@RestController
class GlazedHamController (val configuration: BotConfiguration) {

    var states = mutableMapOf<String, String>()

    @RequestMapping("/")
    fun index(): String {
        return "Hello from GlazedHamBot"
    }

    @RequestMapping("/spotify", params = ["state"])
    fun spotify(@RequestParam("state")stateAmount: String): RedirectView {
        val clientId = "client_id=${configuration.client_id}"
        val responseType = "response_type=code"
        // val redirectUri = "redirect_uri=https://bot.rebellious.uno/callback"
        val redirectUri =  "redirect_uri=http://localhost/callback"
        val scope = "scope=user-read-playback-state"
        val state = "state=$stateAmount"
        val authUrl = "https://accounts.spotify.com/authorize?$clientId&$responseType&$redirectUri&$scope&$state"
        val redirectView = RedirectView()
        redirectView.url = authUrl
        states[stateAmount.toString()] = ""
        return redirectView
    }

    @RequestMapping("/spotify")
    fun spotify(): RedirectView {
        val stateAmount = SecureRandom.getInstanceStrong().nextInt(32000).toString()
        return spotify(stateAmount)
    }

    @RequestMapping("/callback")
    fun saveAuthToken(@RequestParam("code") code: String, @RequestParam("state") state: String): String {
        Logger.getLogger(GlazedHamController::class.java.toString()).info("$state - $code")
        return if (states.containsKey(state)){
            addAuthCode(code, state)
            "Thank you for Authorising"
        }
        else
            "Sorry I don't recognise you."
    }


    private fun addAuthCode(authCode: String, state: String) {
        val url = "jdbc:sqlite:../GlazedHamBot/${state.toLowerCase()}.db"
        val fileExists = File("../GlazedHamBot/$state.db").exists()
        if (fileExists) {
            val con = DriverManager.getConnection(url)
            con.use { con ->
                createSpotifyDb(con)
                addAuthCodeToDb(con, authCode)
            }
        }
    }

    private fun addAuthCodeToDb(con: Connection, authCode: String) {
        val countSql = "select count(*) from spotifysettings"
        val countResult = con.createStatement().executeQuery(countSql)

        val c = if (countResult.next())
            countResult.getInt(1)
        else 0

        val sql = if (c > 0)
            "update spotifysettings set authCode = ? where ROWID=1"
        else
            "insert into spotifysettings (authCode) values (?)"
        Logger.getLogger(GlazedHamController::class.java.toString()).info(sql)
        con.prepareStatement(sql).apply {
            setString(1, authCode)
            executeUpdate()
        }
    }


    private fun createSpotifyDb(con: Connection) {
        val createTable = """CREATE TABLE if not exists spotifysettings (
            authCode	TEXT NOT NULL,
            refreshToken	TEXT,
            accessToken	TEXT,
            expires	NUMERIC)"""
        con.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(createTable)
        }
    }
}