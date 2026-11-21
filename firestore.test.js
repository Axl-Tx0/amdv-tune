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
