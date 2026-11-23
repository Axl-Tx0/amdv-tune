const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_amdv_777";
const ADMIN_EMAIL = "amdvtune@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext(verified = true) {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: verified,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles, tracks, or global catalog", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tracks").get()
  );
  await assertFails(
    unauthDb.collection("catalog_artists").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("catalog_albums").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("catalog_tracks").where("published", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      themeMode: "NEGRO",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated owner can create and partially update UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      themeMode: "NEGRO",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "NEON",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "ROJO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Spoof",
      themeMode: "NEGRO",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Authenticated owner can create, list, update favorite, and delete UserTrack including CLOUD sourceType", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tracks").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_01")
      .set({
        userId: ALICE_UID,
        title: "Neon Pulse",
        artist: "AMDV",
        durationSec: 12,
        favorite: false,
        sourceType: "CLOUD",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_01")
      .update({
        favorite: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_02")
      .set({
        id: "track_02",
        userId: ALICE_UID,
        title: "Bad Payload",
        artist: "Tester",
        durationSec: 15,
        favorite: false,
        sourceType: "LOCAL",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_03")
      .set({
        userId: ALICE_UID,
        title: "Future Track",
        artist: "Tester",
        durationSec: 15,
        favorite: false,
        sourceType: "LOCAL",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_01")
      .delete()
  );
});

test("Cross-user isolation prevents Bob from reading or writing Alice tracks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const now = new Date(Date.now() - 10000);
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      themeMode: "NEGRO",
      createdAt: now,
      updatedAt: now,
    });
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_01")
      .set({
        userId: ALICE_UID,
        title: "Alice Track",
        artist: "AMDV",
        durationSec: 12,
        favorite: true,
        sourceType: "BUNDLED",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tracks").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_01")
      .update({ favorite: false, updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("tracks")
      .doc("track_01")
      .delete()
  );
});

test("Global Catalog: Verified Admin can publish artists, albums, and tracks; unverified spoof and normal users cannot write", async () => {
  const adminDb = adminContext(true).firestore();
  const spoofDb = adminContext(false).firestore();
  const userDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@gmail.com",
      email_verified: true,
    })
    .firestore();
  const created = new Date(Date.now() - 10000);

  // Spoofed unverified admin email must be rejected
  await assertFails(
    spoofDb.collection("catalog_artists").doc("artist_1").set({
      userId: ADMIN_UID,
      name: "Spoof Artist",
      bio: "Bio",
      photoUrl: "",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Regular user cannot create artist
  await assertFails(
    userDb.collection("catalog_artists").doc("artist_1").set({
      userId: ALICE_UID,
      name: "Normal User Artist",
      bio: "Bio",
      photoUrl: "",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Verified Admin creates artist
  await assertSucceeds(
    adminDb.collection("catalog_artists").doc("artist_1").set({
      userId: ADMIN_UID,
      name: "AMDV Collective",
      bio: "Rock y Synth desde Argentina",
      photoUrl: "https://example.com/artist.jpg",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Orphaned album with non-existent artistId must fail
  await assertFails(
    adminDb.collection("catalog_albums").doc("album_bad").set({
      userId: ADMIN_UID,
      artistId: "missing_artist",
      artistName: "Ghost",
      title: "Orphan Album",
      releaseYear: 2026,
      coverUrl: "",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Verified Admin creates album referencing artist_1
  await assertSucceeds(
    adminDb.collection("catalog_albums").doc("album_1").set({
      userId: ADMIN_UID,
      artistId: "artist_1",
      artistName: "AMDV Collective",
      title: "Noches de Neón",
      releaseYear: 2026,
      coverUrl: "https://example.com/album.jpg",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Verified Admin creates track referencing artist_1 and album_1
  await assertSucceeds(
    adminDb.collection("catalog_tracks").doc("ctrack_1").set({
      userId: ADMIN_UID,
      artistId: "artist_1",
      artistName: "AMDV Collective",
      albumId: "album_1",
      albumTitle: "Noches de Neón",
      title: "Pulso Argentino",
      durationSec: 215,
      trackNumber: 1,
      audioUrl: "https://example.com/pulso.mp3",
      coverUrl: "https://example.com/album.jpg",
      lyrics: "Letra de prueba",
      published: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Signed-in normal user can query published artists, albums, and tracks
  await assertSucceeds(
    userDb.collection("catalog_artists").where("published", "==", true).get()
  );
  await assertSucceeds(
    userDb.collection("catalog_albums").where("published", "==", true).get()
  );
  await assertSucceeds(
    userDb.collection("catalog_tracks").where("published", "==", true).get()
  );

  // Unfiltered query by normal user fails (rules enforce published == true)
  await assertFails(userDb.collection("catalog_tracks").get());

  // Normal user cannot modify or delete admin catalog items
  await assertFails(
    userDb
      .collection("catalog_tracks")
      .doc("ctrack_1")
      .update({ title: "Hacked", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    userDb.collection("catalog_tracks").doc("ctrack_1").delete()
  );

  // Admin can update artist, album, and track without deleting/re-uploading
  await assertSucceeds(
    adminDb.collection("catalog_artists").doc("artist_1").update({
      name: "AMDV Collective Editado",
      bio: "Bio actualizada",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("catalog_albums").doc("album_1").update({
      title: "Noches de Neón Deluxe",
      releaseYear: 2027,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    adminDb.collection("catalog_tracks").doc("ctrack_1").update({
      title: "Pulso Argentino (Remaster)",
      trackNumber: 2,
      lyrics: "[00:05.00] Nueva letra sincronizada",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin can delete track, album, artist
  await assertSucceeds(
    adminDb.collection("catalog_tracks").doc("ctrack_1").delete()
  );
  await assertSucceeds(
    adminDb.collection("catalog_albums").doc("album_1").delete()
  );
  await assertSucceeds(
    adminDb.collection("catalog_artists").doc("artist_1").delete()
  );
});

test("UserPlaylist: Owner can CRUD playlists; unauthenticated and other users are denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .set({
        userId: ALICE_UID,
        name: "Entrenamiento",
        description: "Mis temas para correr",
        trackIds: ["trk_1", "trk_2"],
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .update({
        name: "Entrenamiento 2026",
        trackIds: ["trk_1", "trk_2", "trk_3"],
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject empty playlist name or immutable createdAt mutation
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .update({
        name: "",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Cross-user isolation: Bob cannot read, update, or delete Alice's playlists
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("playlists").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .update({ name: "Robada", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .delete()
  );

  // Owner can delete playlist
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("playlists")
      .doc("pl_gym_01")
      .delete()
  );
});

test("CommunityStats & UserReactions: bounded increments, owner-only reactions, rejects abuse", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Unauthenticated denied
  await assertFails(unauthDb.collection("community_stats").get());

  // Alice creates initial community stats for trk_1 with playCount = 1
  await assertSucceeds(
    aliceDb.collection("community_stats").doc("trk_1").set({
      trackId: "trk_1",
      playCount: 1,
      fireCount: 0,
      heartCount: 0,
      headphonesCount: 0,
      updatedAt: now,
    })
  );

  // Bob increments playCount by +1 and fireCount by +1
  await assertSucceeds(
    bobDb.collection("community_stats").doc("trk_1").update({
      playCount: 2,
      fireCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob toggles fireCount back down by -1
  await assertSucceeds(
    bobDb.collection("community_stats").doc("trk_1").update({
      fireCount: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject abusive inflation (+50 plays)
  await assertFails(
    bobDb.collection("community_stats").doc("trk_1").update({
      playCount: 52,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject decrementing playCount
  await assertFails(
    bobDb.collection("community_stats").doc("trk_1").update({
      playCount: 1,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Normal user cannot delete community stats
  await assertFails(bobDb.collection("community_stats").doc("trk_1").delete());

  // Per-user reactions are strictly owner-scoped
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reactions")
      .doc("trk_1")
      .set({
        userId: ALICE_UID,
        trackId: "trk_1",
        fire: true,
        heart: false,
        headphones: false,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reactions")
      .doc("trk_1")
      .update({
        heart: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reactions")
      .doc("trk_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reactions")
      .doc("trk_1")
      .update({ fire: false, updatedAt: new Date(Date.now() - 1000) })
  );
});

test("JamSessions: host manages playback, guest joins by 6-char code and adds to queue, list denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Invalid room code rejected
  await assertFails(
    aliceDb.collection("jam_sessions").doc("bad").set({
      roomCode: "bad",
      hostUserId: ALICE_UID,
      hostName: "Alice",
      currentTrackId: "trk_1",
      playing: true,
      positionMs: 0,
      queueTrackIds: [],
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice creates valid 6-char Jam room
  await assertSucceeds(
    aliceDb.collection("jam_sessions").doc("AMDV99").set({
      roomCode: "AMDV99",
      hostUserId: ALICE_UID,
      hostName: "Alice",
      currentTrackId: "trk_1",
      playing: true,
      positionMs: 12000,
      queueTrackIds: ["trk_2"],
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Listing all rooms is forbidden
  await assertFails(bobDb.collection("jam_sessions").get());

  // Bob can read room by exact code
  await assertSucceeds(bobDb.collection("jam_sessions").doc("AMDV99").get());

  // Bob can append to shared queue while active
  await assertSucceeds(
    bobDb.collection("jam_sessions").doc("AMDV99").update({
      queueTrackIds: ["trk_2", "trk_3"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot hijack playback or hostUserId or delete room
  await assertFails(
    bobDb.collection("jam_sessions").doc("AMDV99").update({
      playing: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("jam_sessions").doc("AMDV99").delete());

  // Alice (host) can update playback and close/delete room
  await assertSucceeds(
    aliceDb.collection("jam_sessions").doc("AMDV99").update({
      currentTrackId: "trk_2",
      playing: false,
      positionMs: 0,
      active: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(aliceDb.collection("jam_sessions").doc("AMDV99").delete());
});

test("SharedPlaylists: share by 6-char code, collaborative edit guard, list denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Alice publishes a collaborative shared playlist
  await assertSucceeds(
    aliceDb.collection("shared_playlists").doc("MIX777").set({
      shareCode: "MIX777",
      ownerUserId: ALICE_UID,
      ownerName: "Alice",
      name: "Fiesta AMDV",
      description: "Para todos",
      trackIds: ["trk_1"],
      collaborative: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Listing all shared playlists is denied
  await assertFails(bobDb.collection("shared_playlists").get());

  // Bob can fetch by exact code and add tracks while collaborative == true
  await assertSucceeds(bobDb.collection("shared_playlists").doc("MIX777").get());
  await assertSucceeds(
    bobDb.collection("shared_playlists").doc("MIX777").update({
      trackIds: ["trk_1", "trk_2"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot rename Alice's shared playlist
  await assertFails(
    bobDb.collection("shared_playlists").doc("MIX777").update({
      name: "Secuestrada",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice turns collaborative off -> Bob can no longer edit trackIds
  await assertSucceeds(
    aliceDb.collection("shared_playlists").doc("MIX777").update({
      collaborative: false,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    bobDb.collection("shared_playlists").doc("MIX777").update({
      trackIds: ["trk_1", "trk_2", "trk_3"],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Owner can delete
  await assertSucceeds(
    aliceDb.collection("shared_playlists").doc("MIX777").delete()
  );
});

test("ArtistFollows and ArtistStats: owner-scoped follows and bounded +/-1 followerCount", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Unauthenticated denied
  await assertFails(unauthDb.collection("artist_stats").doc("art_1").get());

  // Alice follows art_1
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("followed_artists")
      .doc("art_1")
      .set({
        userId: ALICE_UID,
        artistId: "art_1",
        artistName: "AMDV Collective",
        followedAt: now,
      })
  );

  // Bob cannot read or write Alice's followed artists
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("followed_artists").get()
  );

  // Alice initializes artist_stats with 1 follower
  await assertSucceeds(
    aliceDb.collection("artist_stats").doc("art_1").set({
      artistId: "art_1",
      followerCount: 1,
      updatedAt: now,
    })
  );

  // Bob increments by +1
  await assertSucceeds(
    bobDb.collection("artist_stats").doc("art_1").update({
      followerCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Inflating followerCount by +50 must fail
  await assertFails(
    bobDb.collection("artist_stats").doc("art_1").update({
      followerCount: 52,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Unfollow decrement by -1 succeeds and Alice can delete her follow record
  await assertSucceeds(
    aliceDb.collection("artist_stats").doc("art_1").update({
      followerCount: 1,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("followed_artists")
      .doc("art_1")
      .delete()
  );
});

test("TrackComments: signed-in users create timestamped comments, author or Admin can delete, Bob cannot delete Alice's", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = adminContext(true).firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("track_comments").get());

  // Alice posts a comment at 01:42 (102000 ms)
  await assertSucceeds(
    aliceDb.collection("track_comments").doc("cmt_1").set({
      trackId: "trk_1",
      trackTitle: "Neon Pulse",
      userId: ALICE_UID,
      authorName: "Alice",
      timestampMs: 102000,
      text: "Este coro es increíble!",
      createdAt: now,
    })
  );

  // Spoofing another user's UID fails
  await assertFails(
    bobDb.collection("track_comments").doc("cmt_spoof").set({
      trackId: "trk_1",
      trackTitle: "Neon Pulse",
      userId: ALICE_UID,
      authorName: "Fake Alice",
      timestampMs: 5000,
      text: "Spoof",
      createdAt: now,
    })
  );

  // Overly long comment (> 220 chars) fails
  await assertFails(
    bobDb.collection("track_comments").doc("cmt_long").set({
      trackId: "trk_1",
      trackTitle: "Neon Pulse",
      userId: BOB_UID,
      authorName: "Bob",
      timestampMs: 5000,
      text: "a".repeat(225),
      createdAt: now,
    })
  );

  // Comments are immutable (update denied)
  await assertFails(
    aliceDb.collection("track_comments").doc("cmt_1").update({
      text: "Edited",
    })
  );

  // Bob can read track comments by trackId, but cannot delete Alice's comment
  await assertSucceeds(
    bobDb.collection("track_comments").where("trackId", "==", "trk_1").get()
  );
  await assertFails(bobDb.collection("track_comments").doc("cmt_1").delete());

  // Verified Admin can moderate/delete Alice's comment
  await assertSucceeds(adminDb.collection("track_comments").doc("cmt_1").delete());

  // Bob creates his own comment and deletes it himself
  await assertSucceeds(
    bobDb.collection("track_comments").doc("cmt_bob").set({
      trackId: "trk_1",
      trackTitle: "Neon Pulse",
      userId: BOB_UID,
      authorName: "Bob",
      timestampMs: 32000,
      text: "Buen ritmo!",
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(bobDb.collection("track_comments").doc("cmt_bob").delete());
});


