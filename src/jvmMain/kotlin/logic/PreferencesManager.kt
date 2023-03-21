package logic

import java.util.prefs.Preferences

object PreferencesManager {

    private val preferences: Preferences = Preferences.userRoot().node("com/loloof64/chess_against_engine")

    fun saveSavePgnFolder(newPath: String) {
        preferences.put("savePgnFolder", newPath)
    }

    fun loadSavePgnFolder(): String {
        return preferences.get("savePgnFolder", "")
    }
}