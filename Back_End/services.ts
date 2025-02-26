import { MongoClient } from 'mongodb';
import admin from "firebase-admin";

export const client = new MongoClient(process.env.DB_URI ?? "mongodb://localhost:27017");

// Initialize Firebase Admin
admin.initializeApp({
    credential: admin.credential.cert({
        projectId: "cmiyc-eaaf9",
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL ?? "",
        privateKey: (process.env.FIREBASE_PRIVATE_KEY ?? "").replace(/\\n/g, '\n'),
    }),
});

export const messaging = admin.messaging();
