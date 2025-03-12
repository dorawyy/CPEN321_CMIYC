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

// Test user ID and friend ID
const TEST_USER_ID = testUserData.userID;
const TEST_FRIEND_ID = "test-friend-123";
const TEST_FRIEND_EMAIL = "friend@example.com";

beforeAll(async () => {
  app = setupTestApp();
  // Connect to the actual database for non-mocked tests
  try {
    await client.connect();

    // Create a test user with friends array and friendRequests array
    await client.db("cmiyc").collection("users").insertOne({
      ...testUserData,
      friends: [],
      friendRequests: []
    });

    // Create a test friend
    await client.db("cmiyc").collection("users").insertOne({
      userID: TEST_FRIEND_ID,
      displayName: "Test Friend",
      email: TEST_FRIEND_EMAIL,
      photoURL: "https://example.com/friend.jpg",
      fcmToken: "test-friend-fcm-token",
      currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
      isAdmin: false,
      friends: [],
      friendRequests: []
    });
  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
}, 3000); // 3 seconds timeout for beforeAll

afterAll(async () => {
  // Clean up test data
  try {
    await client.db("cmiyc").collection("users").deleteOne({ userID: TEST_USER_ID });
    await client.db("cmiyc").collection("users").deleteOne({ userID: TEST_FRIEND_ID });
    await client.close();
  } catch (error) {
    console.error('Error in cleanup:', error);
  }
}, 3000); // 3 seconds timeout for afterAll

/**
 * Tests for FriendRoutes without mocking external dependencies
 * These tests use the actual database connection
 */
describe('FriendRoutes API - No Mocks', () => {

  /**
   * GET /friends/:userID - Get User's Friends
   * Tests the route for retrieving a user's friends
   */
  describe('GET /friends/:userID - Get Friends', () => {
    
    /**
     * Test: Successfully get friends for user with no friends
     * Input: Valid userID with empty friends list
     * Expected Status: 200
     * Expected Output: Empty array
     * Expected Behavior: Should return empty array for user with no friends
     */
    test('should return empty array for user with no friends', async () => {
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBe(0);
    });
    
    /**
     * Test: Try to get friends for non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: Should return 404 for non-existent user
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .get(`/friends/${nonExistentUserID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
    });
  });

  /**
   * POST /friends/:userID/sendRequest/:friendEmail - Send Friend Request
   * Tests the route for sending a friend request
   */
  describe('POST /friends/:userID/sendRequest/:friendEmail - Send Friend Request', () => {
    
    /**
     * Test: Successfully send friend request
     * Input: Valid userID and friendEmail
     * Expected Status: 200
     * Expected Output: Success message
     * Expected Behavior: Friend request should be added to friend's friendRequests array
     */
    test('should send friend request successfully', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request sent successfully');
      
      // Verify friend request was added to friend's friendRequests array
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      expect(friend?.friendRequests).toContain(TEST_USER_ID);
    });
    
    /**
     * Test: Trying to send friend request to yourself
     * Input: UserID and own email
     * Expected Status: 400
     * Expected Output: Error message
     * Expected Behavior: Should return error for sending request to yourself
     */
    test('should return 400 when sending request to yourself', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${testUserData.email}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('Cannot send friend request to yourself');
    });
    
    /**
     * Test: Sending duplicate friend request
     * Input: Valid userID and friendEmail for request already sent
     * Expected Status: 200
     * Expected Output: Already sent message
     * Expected Behavior: Should notify that request was already sent
     */
    test('should return 200 when request already sent', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('You have already sent a friend request to this user');
    });
    
    /**
     * Test: Try to send friend request to non-existent user
     * Input: Valid userID but non-existent friendEmail
     * Expected Status: 404
     * Expected Output: Friend not found message
     * Expected Behavior: Should return 404 for non-existent friend
     */
    test('should return 404 for non-existent friend', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/nonexistent@example.com`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('Friend not found');
    });
  });

  /**
   * GET /friends/:userID/friendRequests - Get Friend Requests
   * Tests the route for retrieving a user's friend requests
   */
  describe('GET /friends/:userID/friendRequests - Get Friend Requests', () => {
    
    /**
     * Test: Successfully get friend requests for the friend
     * Input: Valid friendID
     * Expected Status: 200
     * Expected Output: Array with the user who sent the request
     * Expected Behavior: Should return array with friend request from test user
     */
    test('should return friend requests for user', async () => {
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_FRIEND_ID}/friendRequests`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBe(1);
      expect(response.body[0].userID).toBe(TEST_USER_ID);
      
      // Verify private fields are not included
      expect(response.body[0].friends).toBeUndefined();
      expect(response.body[0].friendRequests).toBeUndefined();
    });
    
    /**
     * Test: Try to get friend requests for non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: Should return 404 for non-existent user
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .get(`/friends/${nonExistentUserID}/friendRequests`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
    });
  });

  /**
   * POST /friends/:userID/acceptRequest/:friendID - Accept Friend Request
   * Tests the route for accepting a friend request
   */
  describe('POST /friends/:userID/acceptRequest/:friendID - Accept Friend Request', () => {
    
    /**
     * Test: Successfully accept friend request
     * Input: Valid friendID and userID
     * Expected Status: 200
     * Expected Output: Success message
     * Expected Behavior: Users should be added to each other's friends arrays
     */
    test('should accept friend request successfully', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_FRIEND_ID}/acceptRequest/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request responded to successfully');
      
      // Verify users are now friends
      const user = await client.db("cmiyc").collection("users").findOne({ userID: TEST_USER_ID });
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      
      expect(user?.friends).toContain(TEST_FRIEND_ID);
      expect(friend?.friends).toContain(TEST_USER_ID);
      
      // Verify friend request was removed
      expect(friend?.friendRequests).not.toContain(TEST_USER_ID);
    });
    
    /**
     * Test: Try to accept non-existent friend request
     * Input: Valid userIDs but no friend request exists
     * Expected Status: 400
     * Expected Output: No friend request found message
     * Expected Behavior: Should return error for non-existent request
     */
    test('should return 400 for non-existent friend request', async () => {
      // Since the request was already accepted, trying again should fail
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_FRIEND_ID}/acceptRequest/${TEST_USER_ID}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('No friend request found');
    });
    
    /**
     * Test: Try to accept friend request with non-existent user
     * Input: Invalid userID or friendID
     * Expected Status: 404
     * Expected Output: User or friend not found message
     * Expected Behavior: Should return 404 for non-existent user
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .post(`/friends/${nonExistentUserID}/acceptRequest/${TEST_USER_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User or friend not found');
    });
  });

  /**
   * PUT /friends/:userID/deleteFriend/:friendID - Delete Friend
   * Tests the route for deleting a friend
   */
  describe('PUT /friends/:userID/deleteFriend/:friendID - Delete Friend', () => {
    
    /**
     * Test: Successfully delete friend
     * Input: Valid userID and friendID who are friends
     * Expected Status: 200
     * Expected Output: Success message
     * Expected Behavior: Users should be removed from each other's friends arrays
     */
    test('should delete friend successfully', async () => {
      const response = await createTestRequest(app)
        .put(`/friends/${TEST_USER_ID}/deleteFriend/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend deleted successfully');
      
      // Verify users are no longer friends
      const user = await client.db("cmiyc").collection("users").findOne({ userID: TEST_USER_ID });
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      
      expect(user?.friends).not.toContain(TEST_FRIEND_ID);
      expect(friend?.friends).not.toContain(TEST_USER_ID);
    });
    
    /**
     * Test: Try to delete non-existent friend
     * Input: Valid userID but non-existent friendID
     * Expected Status: 404
     * Expected Output: User or friend not found message
     * Expected Behavior: Should return 404 for non-existent friend
     */
    test('should return 404 for non-existent user or friend', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .put(`/friends/${TEST_USER_ID}/deleteFriend/${nonExistentUserID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User or friend not found');
    });
  });

  /**
   * POST /friends/:userID/declineRequest/:friendID - Decline Friend Request
   * Tests the route for declining a friend request
   * Note: We need to create a new request first since we accepted the previous one
   */
  describe('POST /friends/:userID/declineRequest/:friendID - Decline Friend Request', () => {
    
    // Setup: Send a new friend request for this test
    beforeAll(async () => {
      // Reset friend request state for these tests
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_FRIEND_ID },
        { $set: { friendRequests: [] } }
      );
      
      // Send a new friend request
      await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
    }, 3000);
    
    /**
     * Test: Successfully decline friend request
     * Input: Valid userID and friendID
     * Expected Status: 200
     * Expected Output: Success message
     * Expected Behavior: Friend request should be removed from friendRequests array
     */
    test('should decline friend request successfully', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_FRIEND_ID}/declineRequest/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request declined successfully');
      
      // Verify friend request was removed
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      expect(friend?.friendRequests).not.toContain(TEST_USER_ID);
      
      // Verify users are not friends
      const user = await client.db("cmiyc").collection("users").findOne({ userID: TEST_USER_ID });
      expect(user?.friends).not.toContain(TEST_FRIEND_ID);
    });
    
    /**
     * Test: Try to decline friend request for non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Expected Behavior: Should return 404 for non-existent user
     */
    test('should return 404 for non-existent user', async () => {
      const nonExistentUserID = 'non-existent-user-' + new ObjectId().toString();
      
      const response = await createTestRequest(app)
        .post(`/friends/${nonExistentUserID}/declineRequest/${TEST_USER_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
    });
  });
}); 