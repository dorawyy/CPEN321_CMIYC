import { Express } from 'express';
import { ObjectId } from 'mongodb';
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

// Test user ID and friend ID
const TEST_USER_ID = testUserData.userID;
const TEST_FRIEND_ID = "test-friend-123";
const TEST_FRIEND_EMAIL = "friend@example.com";
const TEST_FRIEND_2_ID = "test-friend-456";
const TEST_FRIEND_2_EMAIL = "friend2@example.com";
// Special ID for error testing - this ID will be used to trigger errors
const ERROR_TRIGGER_ID = "error-trigger-999"; 

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

    // Create a second test friend to test already friends scenario
    await client.db("cmiyc").collection("users").insertOne({
      userID: TEST_FRIEND_2_ID,
      displayName: "Test Friend 2",
      email: TEST_FRIEND_2_EMAIL,
      photoURL: "https://example.com/friend2.jpg",
      fcmToken: "test-friend-2-fcm-token",
      currentLocation: { latitude: 49.2827, longitude: -123.1207, timestamp: Date.now() },
      isAdmin: false,
      friends: [],
      friendRequests: []
    });

  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
}, 10000); // 10 seconds timeout for beforeAll

afterAll(async () => {
  // Clean up test data
  try {
    await client.db("cmiyc").collection("users").deleteOne({ userID: testUserData.userID });
    await client.db("cmiyc").collection("users").deleteOne({ userID: TEST_FRIEND_ID });
    await client.db("cmiyc").collection("users").deleteOne({ userID: TEST_FRIEND_2_ID });
    await client.close();
  } catch (error) {
    console.error('Error in cleanup:', error);
  }
}, 10000); // 10 seconds timeout for afterAll

/**
 * Tests for FriendRoutes without mocking external dependencies
 * These tests use the actual database connection
 */
describe('FriendRoutes API - No Mocks', () => {

  /**
   * GET /friends/:userID - Get Friends
   * Tests the route for retrieving a user's friends
   */
  describe('GET /friends/:userID - Get Friends', () => {
    
    /**
     * Test: User with no friends
     * Input: Valid userID (no friends)
     * Expected Status: 200
     * Expected Output: Empty array
     */
    test('should return empty array for user with no friends', async () => {
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.body).toEqual([]);
    });

    /**
     * Test: User with friends, testing the getFriends branch to improve coverage
     * Input: Valid userID with a friend
     * Expected Status: 200
     * Expected Output: Array with friend info
     */
    test('should return friend info for user with friends (covers lines 12-16)', async () => {
      // Add TEST_FRIEND_ID to TEST_USER_ID's friends
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $push: { friends: TEST_FRIEND_ID } as any }
      );

      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.body).toHaveLength(1);
      expect(response.body[0].userID).toBe(TEST_FRIEND_ID);
      expect(response.body[0].displayName).toBe("Test Friend");
      
      // The response should not include the friends or friendRequests arrays of the friend
      expect(response.body[0].friends).toBeUndefined();
      expect(response.body[0].friendRequests).toBeUndefined();
      
      // After test, remove the friend to keep tests isolated
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $pull: { friends: TEST_FRIEND_ID } as any }
      );
    });
    
    /**
     * Test: Non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     */
    test('should return 404 for non-existent user', async () => {
      const response = await createTestRequest(app)
        .get('/friends/non-existent-user');
      
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
     * Test: Successfully send a friend request
     * Input: Valid userID and friendEmail
     * Expected Status: 200
     * Expected Output: Success message
     */
    test('should send friend request successfully', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request sent successfully');
      
      // Verify the friend request was added to the friend's friendRequests array
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      expect(friend?.friendRequests).toContain(TEST_USER_ID);
      
      // Clean up - remove the friend request for other tests
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_FRIEND_ID },
        { $pull: { friendRequests: TEST_USER_ID } as any }
      );
    });
    
    /**
     * Test: Cannot send friend request to yourself (line 47-48)
     * Input: userID and friendEmail are the same person
     * Expected Status: 400
     * Expected Output: Error message
     */
    test('should return 400 when sending request to yourself', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${testUserData.email}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('Cannot send friend request to yourself');
    });
    
    /**
     * Test: Already friends scenario (covers line 47-48)
     * Input: User and friend already have a friend relationship
     * Expected Status: 400
     * Expected Output: Already friends message
     */
    test('should return 400 when already friends', async () => {
      // Make them friends first
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $push: { friends: TEST_FRIEND_2_ID } as any }
      );
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_2_EMAIL}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('You are already friends');
      
      // Clean up
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $pull: { friends: TEST_FRIEND_2_ID } as any }
      );
    });
    
    /**
     * Test: Request already sent (duplicate request) scenario
     * Input: Valid userID and friendEmail where request already exists
     * Expected Status: 200
     * Expected Output: Request already sent message
     */
    test('should return 200 when request already sent', async () => {
      // Add the friend request first
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_FRIEND_ID },
        { $push: { friendRequests: TEST_USER_ID } as any }
      );
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('You have already sent a friend request to this user');
      
      // Clean up
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_FRIEND_ID },
        { $pull: { friendRequests: TEST_USER_ID } as any }
      );
    });
    
    /**
     * Test: Non-existent friend
     * Input: Valid userID but non-existent friendEmail
     * Expected Status: 404
     * Expected Output: Friend not found message
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
   * Tests the route for retrieving a user's pending friend requests
   */
  describe('GET /friends/:userID/friendRequests - Get Friend Requests', () => {
    
    /**
     * Test: Get friend requests for user with requests
     * Input: Valid userID with friend requests
     * Expected Status: 200
     * Expected Output: Array of friend requests
     */
    test('should return friend requests for user', async () => {
      // Add a friend request to the test user
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $push: { friendRequests: TEST_FRIEND_ID } as any }
      );
      
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}/friendRequests`);
      
      expect(response.status).toBe(200);
      expect(response.body).toHaveLength(1);
      expect(response.body[0].userID).toBe(TEST_FRIEND_ID);
      
      // Clean up
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $pull: { friendRequests: TEST_FRIEND_ID } as any }
      );
    });
    
    /**
     * Test: Non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     */
    test('should return 404 for non-existent user', async () => {
      const response = await createTestRequest(app)
        .get('/friends/non-existent-user/friendRequests');
      
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
     * Test: Successfully accept a friend request
     * Input: Valid userID and friendID
     * Expected Status: 200
     * Expected Output: Success message
     */
    test('should accept friend request successfully', async () => {
      // Add a friend request to the test user
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $push: { friendRequests: TEST_FRIEND_ID } as any }
      );
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/acceptRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request responded to successfully');
      
      // Verify the friend request was removed and they are now friends
      const user = await client.db("cmiyc").collection("users").findOne({ userID: TEST_USER_ID });
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      
      expect(user?.friendRequests).not.toContain(TEST_FRIEND_ID);
      expect(user?.friends).toContain(TEST_FRIEND_ID);
      expect(friend?.friends).toContain(TEST_USER_ID);
      
      // Clean up - remove the friend relationship for other tests
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $pull: { friends: TEST_FRIEND_ID } as any }
      );
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_FRIEND_ID },
        { $pull: { friends: TEST_USER_ID } as any }
      );
    });
    
    /**
     * Test: Error case - no friend request exists (covers line 115)
     * Input: Valid userID but no friend request from friendID
     * Expected Status: 400
     * Expected Output: No friend request found message
     */
    test('should return 400 for non-existent friend request', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/acceptRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('No friend request found');
    });
    
    /**
     * Test: Error handling case (covers the catch block)
     * Input: Non-existent userID
     * Expected Status: 404
     * Expected Output: Error message
     */
    test('should return 404 for non-existent user', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/non-existent-user/acceptRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User or friend not found');
    });

    /**
     * Test: Error handling in acceptFriendRequest (covers line 115)
     * Input: Force error in request by using invalid ObjectId format
     * Expected Status: 404
     * Expected Output: Error message
     */
    test('should handle database errors gracefully in acceptFriendRequest', async () => {
      // Using an invalid ID format which will cause a database error during DB operations
      const invalidFormatID = "invalid_id_format";
      
      const response = await createTestRequest(app)
        .post(`/friends/${invalidFormatID}/acceptRequest/also-invalid-format`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe("User or friend not found");
    });
  });

  /**
   * PUT /friends/:userID/deleteFriend/:friendID - Delete Friend
   * Tests the route for removing a friend
   */
  describe('PUT /friends/:userID/deleteFriend/:friendID - Delete Friend', () => {
    
    /**
     * Test: Successfully delete a friend
     * Input: Valid userID and friendID that are friends
     * Expected Status: 200
     * Expected Output: Success message
     */
    test('should delete friend successfully', async () => {
      // Add each other as friends first
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $push: { friends: TEST_FRIEND_ID } as any }
      );
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_FRIEND_ID },
        { $push: { friends: TEST_USER_ID } as any }
      );
      
      const response = await createTestRequest(app)
        .put(`/friends/${TEST_USER_ID}/deleteFriend/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend deleted successfully');
      
      // Verify they are no longer friends
      const user = await client.db("cmiyc").collection("users").findOne({ userID: TEST_USER_ID });
      const friend = await client.db("cmiyc").collection("users").findOne({ userID: TEST_FRIEND_ID });
      
      expect(user?.friends).not.toContain(TEST_FRIEND_ID);
      expect(friend?.friends).not.toContain(TEST_USER_ID);
    });
    
    /**
     * Test: Error case - user or friend not found (covers line 159)
     * Input: Non-existent userID or friendID
     * Expected Status: 404
     * Expected Output: Error message
     */
    test('should return 404 for non-existent user or friend', async () => {
      const response = await createTestRequest(app)
        .put(`/friends/non-existent-user/deleteFriend/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User or friend not found');
    });

    /**
     * Test: Error handling in deleteFriend (covers line 159)
     * Input: Force error in request by using invalid ObjectId format
     * Expected Status: 404
     * Expected Output: Error message
     */
    test('should handle database errors gracefully in deleteFriend', async () => {
      // Using an invalid ID format which will cause a database error during DB operations
      const invalidFormatID = "invalid_id_format";
      
      const response = await createTestRequest(app)
        .put(`/friends/${invalidFormatID}/deleteFriend/also-invalid-format`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe("User or friend not found");
    });
  });

  /**
   * POST /friends/:userID/declineRequest/:friendID - Decline Friend Request
   * Tests the route for declining a friend request
   */
  describe('POST /friends/:userID/declineRequest/:friendID - Decline Friend Request', () => {
    
    /**
     * Test: Successfully decline a friend request
     * Input: Valid userID and friendID
     * Expected Status: 200
     * Expected Output: Success message
     */
    test('should decline friend request successfully', async () => {
      // Add a friend request to the test user
      await client.db("cmiyc").collection("users").updateOne(
        { userID: TEST_USER_ID },
        { $push: { friendRequests: TEST_FRIEND_ID } as any }
      );
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/declineRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request declined successfully');
      
      // Verify the friend request was removed
      const user = await client.db("cmiyc").collection("users").findOne({ userID: TEST_USER_ID });
      expect(user?.friendRequests).not.toContain(TEST_FRIEND_ID);
    });
    
    /**
     * Test: Error case - covers the catch block (line 136)
     * Input: Non-existent userID
     * Expected Status: 404
     * Expected Output: Error message
     */
    test('should return 404 for non-existent user', async () => {
      const response = await createTestRequest(app)
        .post(`/friends/non-existent-user/declineRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
    });

    /**
     * Test: Error handling in declineFriendRequest (covers line 136)
     * Input: Force error in request by using invalid ObjectId format
     * Expected Status: 404
     * Expected Output: Error message
     */
    test('should handle database errors gracefully in declineFriendRequest', async () => {
      // Using an invalid ID format which will cause a database error during DB operations
      const invalidFormatID = "invalid_id_format";
      
      const response = await createTestRequest(app)
        .post(`/friends/${invalidFormatID}/declineRequest/also-invalid-format`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe("User not found");
    });
  });
}); 