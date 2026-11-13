package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MaterialPrices
import com.example.data.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "csi_settings")

data class AppSettings(
    val darkTheme: Boolean = true,
    val defaultUnit: UnitSystem = UnitSystem.FEET_INCHES,
    val defaultScale: Int = 50,
    val autoSaveEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = true,
    val defaultGrid: Boolean = true,
    val defaultSnap: Boolean = true,
    val lastActiveProjectId: String = "",
    val offlineModeChosen: Boolean = false,
    val materialPrices: MaterialPrices = MaterialPrices()
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val DEFAULT_SCALE = intPreferencesKey("default_scale")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val DEFAULT_GRID = booleanPreferencesKey("default_grid")
        val DEFAULT_SNAP = booleanPreferencesKey("default_snap")
        val LAST_PROJECT_ID = stringPreferencesKey("last_project_id")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val PRICE_FLOOR_TILE = doublePreferencesKey("price_floor_tile")
        val PRICE_WALL_TILE = doublePreferencesKey("price_wall_tile")
        val PRICE_PAINT = doublePreferencesKey("price_paint")
        val PRICE_CEMENT = doublePreferencesKey("price_cement")
        val PRICE_BRICK = doublePreferencesKey("price_brick")
        val PRICE_DOOR = doublePreferencesKey("price_door")
        val PRICE_WINDOW = doublePreferencesKey("price_window")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val unitName = prefs[Keys.DEFAULT_UNIT] ?: UnitSystem.FEET_INCHES.name
        val unit = UnitSystem.entries.find { it.name == unitName } ?: UnitSystem.FEET_INCHES
        AppSettings(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            defaultUnit = unit,
            defaultScale = prefs[Keys.DEFAULT_SCALE] ?: 50,
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: true,
            defaultGrid = prefs[Keys.DEFAULT_GRID] ?: true,
            defaultSnap = prefs[Keys.DEFAULT_SNAP] ?: true,
            lastActiveProjectId = prefs[Keys.LAST_PROJECT_ID] ?: "",
            offlineModeChosen = prefs[Keys.OFFLINE_MODE] ?: false,
            materialPrices = MaterialPrices(
                floorTilePerSqM = prefs[Keys.PRICE_FLOOR_TILE] ?: 45.0,
                wallTilePerSqM = prefs[Keys.PRICE_WALL_TILE] ?: 38.0,
                paintPerLiter = prefs[Keys.PRICE_PAINT] ?: 14.0,
                cementPerBag = prefs[Keys.PRICE_CEMENT] ?: 9.5,
                brickPerUnit = prefs[Keys.PRICE_BRICK] ?: 0.35,
                doorUnitPrice = prefs[Keys.PRICE_DOOR] ?: 240.0,
                windowUnitPrice = prefs[Keys.PRICE_WINDOW] ?: 185.0,
                currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "$"
            )
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setDefaultUnit(unit: UnitSystem) {
        context.dataStore.edit { it[Keys.DEFAULT_UNIT] = unit.name }
    }

    suspend fun setDefaultScale(scale: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SCALE] = scale }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    suspend fun setDefaultGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_GRID] = enabled }
    }

    suspend fun setDefaultSnap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_SNAP] = enabled }
    }

    suspend fun setLastActiveProjectId(projectId: String) {
        context.dataStore.edit { it[Keys.LAST_PROJECT_ID] = projectId }
    }

    suspend fun setOfflineModeChosen(offline: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE] = offline }
    }

    suspend fun saveMaterialPrices(prices: MaterialPrices) {
        context.dataStore.edit {
            it[Keys.PRICE_FLOOR_TILE] = prices.floorTilePerSqM
            it[Keys.PRICE_WALL_TILE] = prices.wallTilePerSqM
            it[Keys.PRICE_PAINT] = prices.paintPerLiter
            it[Keys.PRICE_CEMENT] = prices.cementPerBag
            it[Keys.PRICE_BRICK] = prices.brickPerUnit
            it[Keys.PRICE_DOOR] = prices.doorUnitPrice
            it[Keys.PRICE_WINDOW] = prices.windowUnitPrice
            it[Keys.CURRENCY_SYMBOL] = prices.currencySymbol
        }
    }
}
