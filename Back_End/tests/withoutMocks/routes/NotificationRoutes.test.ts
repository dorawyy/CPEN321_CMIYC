import { Express } from 'express';
import { ObjectId } from 'mongodb';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import { client } from '../../../services';
import '../../setupFirebaseMock'; // Import Firebase mocking

// Make sure environment variables are loaded before Firebase is initialized
import dotenv from 'dotenv';
dotenv.config();

// Set a long timeout for the entire test suite
jest.setTimeout(3000);

// Setup the test app
let app: Express;

beforeAll(async () => {
  app = setupTestApp();
  // Connect to the actual database for non-mocked tests
  try {
    await client.connect();
  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
}, 3000); // 3 seconds timeout for beforeAll

afterAll(async () => {
  // Clean up test data
  try {
    await client.db("cmiyc").collection("users").deleteOne({ userID: testUserData.userID });
    await client.close();
  } catch (error) {
    console.error('Error in cleanup:', error);
  }
}, 3000); // 3 seconds timeout for afterAll

/**
 * Tests for NotificationRoutes without mocking external dependencies
 * These tests use the actual database connection
 */
describe('NotificationRoutes API - No Mocks', () => {

  // Create a test user first that we can use for all tests
  beforeAll(async () => {
    // Create a test user with friends, fcmToken, etc.
    await client.db("cmiyc").collection("users").insertOne({
      ...testUserData,
      friends: [],
      logList: [],
      currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
      fcmToken: "existing-fcm-token"
    });
  }, 3000);

  /**
   * PUT /fcm/:userID - Set FCM Token
   * Tests the route for setting a user's FCM token
   */
  describe('PUT /fcm/:userID - Set FCM Token', () => {
    
    /**
     * Test: Successfully update FCM token
     * Input: Valid userID and FCM token
     * Expected Status: 200
     * Expected Output: Confirmation message
     * Expected Behavior: FCM token should be updated in the database
     */
    test('should update FCM token for existing user', async () => {
      const newFcmToken = "new-fcm-token-" + Date.now();
      
      const response = await createTestRequest(app)
        .put(`/fcm/${testUserData.userID}`)
        .send({ fcmToken: newFcmToken });
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('FCM token set successfully');
      
      // Verify token was updated in database
      const updatedUser = await client.db("cmiyc").collection("users").findOne({ userID: testUserData.userID });
      expect(updatedUser?.fcmToken).toBe(newFcmToken);
    });
    
    /**
     * Test: Try to update FCM token for non-existent user
     * Input: Invalid userID and valid FCM token
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: No database changes
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .put(`/fcm/${nonExistentUserID}`)
        .send({ fcmToken: "some-token" });
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('User not found');
    });
    
    /**
     * Test: Validation error for missing FCM token
     * Input: Valid userID but missing FCM token
     * Expected Status: 400
     * Expected Output: Validation errors
     * Expected Behavior: No database changes
     */
    test('should return validation errors for missing FCM token', async () => {
      const response = await createTestRequest(app)
        .put(`/fcm/${testUserData.userID}`)
        .send({}); // Missing fcmToken
      
      expect(response.status).toBe(400);
      expect(response.body.errors).toBeTruthy();
    });
  });

  /**
   * GET /notifications/:userID - Get Notifications
   * Tests the route for retrieving a user's notifications
   */
  describe('GET /notifications/:userID - Get Notifications', () => {
    
    /**
     * Test: Successfully get notifications for user
     * Input: Valid userID
     * Expected Status: 200
     * Expected Output: Array of notifications (logList)
     * Expected Behavior: Should return the user's logList
     */
    test('should return notifications for existing user', async () => {
      const response = await createTestRequest(app)
        .get(`/notifications/${testUserData.userID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
    });
    
    /**
     * Test: Try to get notifications for non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: No notifications returned
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .get(`/notifications/${nonExistentUserID}`);
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('User not found');
    });
  });

  /**
   * POST /send-event/:userID - Send Event Notification
   * Tests the route for sending event notifications to nearby friends
   * Note: This route is complex to test without mocks because it relies on:
   * 1. Having friends in the database
   * 2. Those friends being nearby (based on location)
   * 3. Firebase Cloud Messaging
   * For more thorough testing, see the mocked tests.
   */
  describe('POST /send-event/:userID - Send Event Notification', () => {
    
    /**
     * Test: Send notification - basic validation
     * Input: Valid userID and event name
     * Expected Status: 200
     * Expected Output: Success message
     * Expected Behavior: Should process without error, though actual notifications
     * may not be sent if user has no nearby friends
     */
    test('should process notification for existing user', async () => {
      // Add location to test user if not already present
      await client.db("cmiyc").collection("users").updateOne(
        { userID: testUserData.userID },
        { $set: { currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() } } }
      );
      
      const response = await createTestRequest(app)
        .post(`/send-event/${testUserData.userID}`)
        .send({ eventName: "Test Event" });
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe('Notification sent successfully');
    });
    
    /**
     * Test: Try to send notification for non-existent user
     * Input: Invalid userID, valid event name
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: No notifications sent
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .post(`/send-event/${nonExistentUserID}`)
        .send({ eventName: "Test Event" });
      
      expect(response.status).toBe(404);
      expect(response.body.message).toBe('User not found');
    });
    
    /**
     * Test: Validation error for missing event name
     * Input: Valid userID but missing event name
     * Expected Status: 400
     * Expected Output: Validation errors
     * Expected Behavior: No notifications sent
     */
    test('should return validation errors for missing event name', async () => {
      const response = await createTestRequest(app)
        .post(`/send-event/${testUserData.userID}`)
        .send({}); // Missing eventName
      
      expect(response.status).toBe(400);
      expect(response.body.errors).toBeTruthy();
    });
  });
}); 