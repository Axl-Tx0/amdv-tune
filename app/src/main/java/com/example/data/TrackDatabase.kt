package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.DownloadedTrackEntity
import com.example.model.LocalTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE deleted = 0 ORDER BY sortOrder ASC")
    fun observeActiveTracks(): Flow<List<LocalTrackEntity>>

    @Query("SELECT * FROM tracks WHERE deleted = 0 ORDER BY sortOrder ASC")
    suspend fun getActiveTracksOnce(): List<LocalTrackEntity>

    @Query("SELECT * FROM tracks WHERE sourceType = 'LOCAL'")
    suspend fun getDeviceTracksOnce(): List<LocalTrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LocalTrackEntity?

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<LocalTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: LocalTrackEntity)

    @Query("UPDATE tracks SET favorite = :favorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, favorite: Boolean)

    @Query("UPDATE tracks SET lyrics = :lyrics WHERE id = :trackId")
    suspend fun updateLyrics(trackId: String, lyrics: String)

    @Query("UPDATE tracks SET deleted = 1 WHERE id = :trackId")
    suspend fun markDeleted(trackId: String)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM tracks")
    suspend fun maxSortOrder(): Int
}

@Dao
interface DownloadedTrackDao {
    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAtMs DESC")
    fun observeDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun getByTrackId(trackId: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: DownloadedTrackEntity)

    @Query("UPDATE downloaded_tracks SET favorite = :favorite WHERE trackId = :trackId")
    suspend fun setFavorite(trackId: String, favorite: Boolean)

    @Query("UPDATE downloaded_tracks SET lyrics = :lyrics WHERE trackId = :trackId")
    suspend fun updateLyrics(trackId: String, lyrics: String)

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)
}

@Database(
    entities = [LocalTrackEntity::class, DownloadedTrackEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloaded_tracks (
                        trackId TEXT NOT NULL PRIMARY KEY,
                        artistId TEXT NOT NULL,
                        artistName TEXT NOT NULL,
                        albumId TEXT NOT NULL,
                        albumTitle TEXT NOT NULL,
                        title TEXT NOT NULL,
                        durationSec INTEGER NOT NULL,
                        trackNumber INTEGER NOT NULL,
                        localAudioPath TEXT NOT NULL,
                        localCoverPath TEXT NOT NULL,
                        lyrics TEXT NOT NULL,
                        favorite INTEGER NOT NULL DEFAULT 0,
                        downloadedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amdv_tune.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

object SeedTracksProvider {
    fun initialTracks(): List<LocalTrackEntity> = listOf(
        LocalTrackEntity(
            id = "track_01",
            title = "Neon Overdrive",
            artist = "AMDV Collective",
            durationSec = 12L,
            sourceType = "BUNDLED",
            audioUri = "track_01",
            coverKey = "COVER_CRIMSON",
            favorite = false,
            lyrics = "[Instrumental Synth-Rock]\n\nLatidos de neón en la ciudad nocturna,\ncada pulso enciende la frecuencia.",
            sortOrder = 1
        ),
        LocalTrackEntity(
            id = "track_02",
            title = "Horizonte Dorado",
            artist = "Solaris Wave",
            durationSec = 14L,
            sourceType = "BUNDLED",
            audioUri = "track_02",
            coverKey = "COVER_SUNSET",
            favorite = false,
            lyrics = "[Melodía Sunset]\n\nLuz cálida sobre el mar en calma,\nacordes suaves al caer el sol.",
            sortOrder = 2
        ),
        LocalTrackEntity(
            id = "track_03",
            title = "Eco en Silencio",
            artist = "Vektor & Kinetix",
            durationSec = 11L,
            sourceType = "BUNDLED",
            audioUri = "track_03",
            coverKey = "MONO_CYAN_V",
            favorite = false,
            lyrics = "[Atmósfera Digital]\n\nResonancia limpia en frecuencia baja,\nsin ruido, solo ritmo constante.",
            sortOrder = 3
        ),
        LocalTrackEntity(
            id = "track_04",
            title = "Cielo Eléctrico",
            artist = "Pulse Runner",
            durationSec = 13L,
            sourceType = "BUNDLED",
            audioUri = "track_04",
            coverKey = "COVER_CYAN_DRIP",
            favorite = false,
            lyrics = "[Electrónica Nocturna]\n\nDestellos cian cruzando el cielo,\ncada nota marca el compás.",
            sortOrder = 4
        ),
        LocalTrackEntity(
            id = "track_05",
            title = "Al Final del Camino",
            artist = "Nocturne Echo",
            durationSec = 15L,
            sourceType = "BUNDLED",
            audioUri = "track_05",
            coverKey = "MONO_AMBER_N",
            favorite = false,
            lyrics = "[Rock Alternativo Instrumental]\n\nPaso firme bajo luces tenues,\nun viaje sonoro hasta el amanecer.",
            sortOrder = 5
        ),
        LocalTrackEntity(
            id = "track_06",
            title = "Desde Adentro",
            artist = "Static Orbit",
            durationSec = 12L,
            sourceType = "BUNDLED",
            audioUri = "track_06",
            coverKey = "MONO_SILVER_S",
            favorite = false,
            lyrics = "[Synth Minimalista]\n\nVibración pura desde el núcleo,\nenergía constante en cada ciclo.",
            sortOrder = 6
        ),
        LocalTrackEntity(
            id = "track_07",
            title = "Estelares - Ella Dijo (Synth Mix)",
            artist = "Estelar Audio",
            durationSec = 11L,
            sourceType = "BUNDLED",
            audioUri = "track_07",
            coverKey = "MONO_VIOLET_E",
            favorite = false,
            lyrics = "[Versión Electrónica]\n\nGradiente violeta en la pista,\narpegios claros y pulso estelar.",
            sortOrder = 7
        ),
        LocalTrackEntity(
            id = "track_08",
            title = "Pulso Tenue",
            artist = "Nightfall Unit",
            durationSec = 13L,
            sourceType = "BUNDLED",
            audioUri = "track_08",
            coverKey = "MONO_EMERALD_P",
            favorite = false,
            lyrics = "[Ambient Beat]\n\nReflejos de neón sobre asfalto húmedo,\nritmo pausado de medianoche.",
            sortOrder = 8
        ),
        LocalTrackEntity(
            id = "track_09",
            title = "Fuego y Metal",
            artist = "Ironline",
            durationSec = 14L,
            sourceType = "BUNDLED",
            audioUri = "track_09",
            coverKey = "COVER_EMBER",
            favorite = false,
            lyrics = "[Cyber Rock]\n\nChispas en la oscuridad,\npotencia rítmica sin límites.",
            sortOrder = 9
        ),
        LocalTrackEntity(
            id = "track_10",
            title = "Gravedad Cero",
            artist = "Nova Circuit",
            durationSec = 12L,
            sourceType = "BUNDLED",
            audioUri = "track_10",
            coverKey = "MONO_INDIGO_G",
            favorite = false,
            lyrics = "[Space Synth]\n\nÓrbita libre en sonido envolvente,\nsenales limpias en el vacío.",
            sortOrder = 10
        )
    )
}
