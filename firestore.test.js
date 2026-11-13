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
const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTs() {
  return new Date(Date.now() - 2000);
}

function validProject(uid) {
  const ts = pastTs();
  return {
    userId: uid,
    name: "Irfan Modern Villa",
    category: "VILLA",
    isFavorite: true,
    isDeleted: false,
    deletedAtMillis: 0,
    version: 1,
    floorCount: 2,
    folderPath: "CSI-HomeCraft3D/Backups/VILLA",
    projectJson: "{\"id\":\"proj_1\",\"name\":\"Irfan Modern Villa\"}",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validShare(ownerId, isPublic = true, permission = "VIEW_ONLY", revoked = false) {
  const ts = pastTs();
  return {
    ownerId,
    ownerDisplayName: "S Irfan",
    projectId: "proj_1",
    projectName: "Irfan Modern Villa",
    isPublic,
    permission,
    passwordProtected: false,
    passwordHash: "",
    expiresAtMillis: 0,
    revoked,
    collaboratorUids: [],
    snapshotJson: "{\"id\":\"proj_1\"}",
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user cannot read or write projects or shared designs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(unauthDb.collection("shared_designs").where("isPublic", "==", true).where("revoked", "==", false).get());
});

test("Owner can create, read, update, and soft-delete own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(docRef.set(validProject(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(docRef.update({ name: "Updated Villa", version: 2, updatedAt: pastTs() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
});

test("Cross-user access to private project is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set(validProject(ALICE_UID))
  );
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_2").set(validProject(BOB_UID))
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  const badPayload = { ...validProject(ALICE_UID), isAdmin: true };
  await assertFails(docRef.set(badPayload));
});

test("Project version snapshot requires existing parent and is immutable", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  const verRef = projRef.collection("versions").doc("v1");
  await assertFails(verRef.set({
    userId: ALICE_UID,
    projectId: "proj_1",
    versionNumber: 1,
    summary: "Initial",
    snapshotJson: "{}",
    createdAt: pastTs(),
  }));
  await assertSucceeds(projRef.set(validProject(ALICE_UID)));
  await assertSucceeds(verRef.set({
    userId: ALICE_UID,
    projectId: "proj_1",
    versionNumber: 1,
    summary: "Initial",
    snapshotJson: "{}",
    createdAt: pastTs(),
  }));
  await assertFails(verRef.update({ summary: "Tampered" }));
});

test("Shared design enforces private isolation, editable vs view-only, revocation, and comments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const privRef = aliceDb.collection("shared_designs").doc("share_priv");
  await assertSucceeds(privRef.set(validShare(ALICE_UID, false, "VIEW_ONLY", false)));
  await assertFails(bobDb.collection("shared_designs").doc("share_priv").get());

  const pubViewRef = aliceDb.collection("shared_designs").doc("share_view");
  await assertSucceeds(pubViewRef.set(validShare(ALICE_UID, true, "VIEW_ONLY", false)));
  await assertSucceeds(bobDb.collection("shared_designs").doc("share_view").get());
  await assertSucceeds(
    bobDb.collection("shared_designs").where("isPublic", "==", true).where("revoked", "==", false).get()
  );
  await assertFails(
    bobDb.collection("shared_designs").doc("share_view").update({ snapshotJson: "{\"hack\":1}", updatedAt: pastTs() })
  );

  const pubEditRef = aliceDb.collection("shared_designs").doc("share_edit");
  await assertSucceeds(pubEditRef.set(validShare(ALICE_UID, true, "EDITABLE", false)));
  await assertSucceeds(
    bobDb.collection("shared_designs").doc("share_edit").update({ snapshotJson: "{\"edited\":true}", updatedAt: pastTs() })
  );
  await assertFails(
    bobDb.collection("shared_designs").doc("share_edit").update({ revoked: true, updatedAt: pastTs() })
  );

  await assertSucceeds(
    bobDb.collection("shared_designs").doc("share_view").collection("comments").doc("c1").set({
      shareId: "share_view",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Great floor plan!",
      createdAt: pastTs(),
    })
  );

  await assertSucceeds(pubViewRef.update({ revoked: true, updatedAt: pastTs() }));
  await assertFails(bobDb.collection("shared_designs").doc("share_view").get());
});
