import { Express } from 'express';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import { client } from '../../../services';
import '../../setupFirebaseMock'; // Import Firebase mocking

// Make sure environment variables are loaded before Firebase is initialized
import dotenv from 'dotenv';
dotenv.config();

// Set a long timeout for the entire test suite
jest.setTimeout(10000);

// Setup the test app
let app: Express;

// Test user ID and banned user ID
const TEST_USER_ID = testUserData.userID;
const BANNED_USER_ID = "banned-user-123";
const NEW_TEST_USER_ID = "new-test-user-456"; // Use a different ID for the new test user
const COMPLETE_TEST_USER = {
  userID: NEW_TEST_USER_ID,
  displayName: "Complete Test User",
  email: "complete@example.com",
  photoURL: "https://example.com/complete.jpg",
  fcmToken: "test-fcm-token-456",
  currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
  isAdmin: false
};

beforeAll(async () => {
  app = setupTestApp();
  // Connect to the actual database for non-mocked tests
  try {
    await client.connect();

    // Create a banned user for testing the isBanned logic
    await client.db("cmiyc").collection("users").insertOne({
      userID: BANNED_USER_ID,
      displayName: "Banned User",
      email: "banned@example.com",
      photoURL: "https://example.com/banned.jpg",
      isAdmin: false,
      isBanned: true,
      friends: [],
      friendRequests: [],
      logList: []
    });

  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
}, 10000); // 10 seconds timeout for beforeAll

afterAll(async () => {
  // Clean up test data
  try {
    // Delete any users created during tests
    await client.db("cmiyc").collection("users").deleteOne({ userID: testUserData.userID });
    await client.db("cmiyc").collection("users").deleteOne({ userID: BANNED_USER_ID });
    await client.db("cmiyc").collection("users").deleteOne({ userID: NEW_TEST_USER_ID });
    await client.close();
  } catch (error) {
    console.error('Error in cleanup:', error);
  }
}, 10000); // 10 seconds timeout for afterAll

/**
 * Tests for UserRoutes without mocking external dependencies
 * These tests use the actual database connection
 */
describe('UserRoutes API - No Mocks', () => {

  /**
   * POST /user - Create User Profile
   * Tests the route for creating a user profile
   */
  describe('POST /user - Create User Profile', () => {
    
    /**
     * Test: Create a new user profile
     * This test directly tests the UserController.createUserProfile method
     * through the API endpoint
     */
    test('should create a new user profile', async () => {
      // Ensure the user doesn't exist first
      await client.db("cmiyc").collection("users").deleteOne({ userID: NEW_TEST_USER_ID });
      
      // Make the API call to create a new user
      const response = await createTestRequest(app)
        .post('/user')
        .send(COMPLETE_TEST_USER);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toContain("User profile created");
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(false);
      
      // Verify the user exists in the database with all expected fields
      const createdUser = await client.db("cmiyc").collection("users").findOne({ userID: NEW_TEST_USER_ID });
      expect(createdUser).not.toBeNull();
      expect(createdUser?.displayName).toBe(COMPLETE_TEST_USER.displayName);
      expect(createdUser?.friends).toEqual([]);
      expect(createdUser?.friendRequests).toEqual([]);
      expect(createdUser?.logList).toEqual([]);
      expect(createdUser?.isBanned).toBe(false);
    });
    
    /**
     * Test: Create profile for banned user (covers line 12 in UserController.ts)
     * Input: Banned user ID
     * Expected Status: 200
     * Expected Output: User is banned message
     */
    test('should handle banned user appropriately', async () => {
      const bannedUserProfile = {
        userID: BANNED_USER_ID,
        displayName: "Still Banned User",
        email: "banned@example.com",
        photoURL: "https://example.com/banned.jpg",
        fcmToken: "banned-fcm-token",
        currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
        isAdmin: false
      };
      
      const response = await createTestRequest(app)
        .post('/user')
        .send(bannedUserProfile);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe("User is banned");
      expect(response.body.isBanned).toBe(true);
      
      // Verify in the database that the user is still banned
      const bannedUser = await client.db("cmiyc").collection("users").findOne({ userID: BANNED_USER_ID });
      expect(bannedUser?.isBanned).toBe(true);
    });
    
    /**
     * Test: Return existing user profile
     * Input: UserID that already exists
     * Expected Status: 200
     * Expected Output: User profile already exists message
     */
    test('should return existing user profile if userID exists', async () => {
      // Ensure the user exists with known values
      await client.db("cmiyc").collection("users").updateOne(
        { userID: NEW_TEST_USER_ID },
        { 
          $set: { 
            displayName: "Existing User",
            email: "existing@example.com",
            photoURL: "https://example.com/existing.jpg",
            isBanned: false,
            isAdmin: false
          } 
        },
        { upsert: true }
      );
      
      // Try to create the user again through the API
      const updatedUserData = {
        userID: NEW_TEST_USER_ID,
        displayName: "Updated User",
        email: "updated@example.com",
        photoURL: "https://example.com/updated.jpg",
        fcmToken: "updated-fcm-token",
        currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
        isAdmin: false
      };
      
      // Make the API call
      const response = await createTestRequest(app)
        .post('/user')
        .send(updatedUserData);
      
      expect(response.status).toBe(200);
      expect(response.body.message).toBe("User profile already exists");
      expect(response.body.isAdmin).toBe(false);
      expect(response.body.isBanned).toBe(false);
      
      // Check directly in the database that the user wasn't updated
      const existingUser = await client.db("cmiyc").collection("users").findOne({ userID: NEW_TEST_USER_ID });
      expect(existingUser).not.toBeNull();
      expect(existingUser?.displayName).toBe("Existing User");
      expect(existingUser?.email).toBe("existing@example.com");
    });
    
    /**
     * Test: Validation errors for missing required fields
     * Input: Incomplete user data
     * Expected Status: 400
     * Expected Output: Validation errors
     */
    test('should return validation errors for missing fields', async () => {
      const incompleteUser = {
        // Missing userID and other required fields
        displayName: "Incomplete User",
        email: "incomplete@example.com"
      };
      
      const response = await createTestRequest(app)
        .post('/user')
        .send(incompleteUser);
      
      expect(response.status).toBe(400);
      // Should have validation errors
      expect(response.body).toHaveProperty('errors');
    });
  });

  /**
   * GET /user/:userID - Get All Users (except requesting user)
   * Tests the route for retrieving all users except the requesting user
   */
  describe('GET /user/:userID - Get All Users', () => {
    
    /**
     * Test: Get all users except requesting user
     * Input: Valid userID
     * Expected Status: 200
     * Expected Output: Array of users excluding the requesting user
     */
    test('should return all users except the requesting user', async () => {
      // Create a couple of test users to ensure there's data to return
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { 
          $set: { 
            displayName: "Test User", 
            email: "test@example.com",
            photoURL: "https://example.com/test.jpg",
            isAdmin: false,
            isBanned: false,
            friends: ["friend1"],
            friendRequests: ["request1"],
            logList: []
          } 
        },
        { upsert: true }
      );
      
      const response = await createTestRequest(app)
        .get(`/user/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      
      // Verify users are filtered correctly
      response.body.forEach((user: any) => {
        expect(user.userID).not.toBe(TEST_USER_ID);
        // No friend lists should be returned
        expect(user.friends).toBeUndefined();
        expect(user.friendRequests).toBeUndefined();
      });
      
      // The banned user should be in the results
      const bannedUserInResults = response.body.some((user: any) => user.userID === BANNED_USER_ID);
      expect(bannedUserInResults).toBe(true);
    });
  });

  /**
   * POST /user/ban/:userID - Ban User
   * Tests the route for banning a user
   */
  describe('POST /user/ban/:userID - Ban User', () => {
    
    /**
     * Test: Successfully ban a user
     * Input: Valid userID to ban
     * Expected Status: 200
     * Expected Output: Success message
     */
    test('should ban a user successfully', async () => {
      // Make sure our test user exists and is not banned
      await client.db("cmiyc").collection("users").updateOne(
        { userID: NEW_TEST_USER_ID },
        { $set: { isBanned: false } },
        { upsert: true }
      );
      
      const response = await createTestRequest(app)
        .post(`/user/ban/${NEW_TEST_USER_ID}`)
        .send();
      
      expect(response.status).toBe(200);
      expect(response.text).toBe("User banned");
      
      // Verify the user is now banned in the database
      const bannedUser = await client.db("cmiyc").collection("users").findOne({ userID: NEW_TEST_USER_ID });
      expect(bannedUser?.isBanned).toBe(true);
    });
    
    /**
     * Test: Attempt to ban a non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = "non-existent-user-999";
      
      // Make sure the user doesn't exist
      await client.db("cmiyc").collection("users").deleteOne({ userID: nonExistentUserID });
      
      const response = await createTestRequest(app)
        .post(`/user/ban/${nonExistentUserID}`)
        .send();
      
      expect(response.status).toBe(404);
      expect(response.text).toBe("User not found");
    });
  });
}); 