import { Express } from 'express';
import { ObjectId } from 'mongodb';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import '../../setupFirebaseMock'; // Import Firebase mocking

// Set a long timeout for the entire test suite
jest.setTimeout(3000);

// Define test IDs
const TEST_USER_ID = testUserData.userID;
const TEST_FRIEND_ID = "test-friend-123";
const TEST_FRIEND_EMAIL = "friend@example.com";

// Mock the client from services
jest.mock('../../../services', () => {
  const mockCollection = {
    findOne: jest.fn(),
    insertOne: jest.fn(),
    updateOne: jest.fn(),
    find: jest.fn(),
  };
  
  const mockDb = {
    collection: jest.fn(() => mockCollection)
  };
  
  return {
    client: {
      connect: jest.fn(),
      close: jest.fn(),
      db: jest.fn(() => mockDb)
    }
  };
});

// Import and extract the mocked client
import { client } from '../../../services';
const mockDb = client.db('cmiyc');
const mockCollection = mockDb.collection('users');

// Setup the test app
let app: Express;

beforeAll(() => {
  app = setupTestApp();
});

beforeEach(() => {
  // Reset all mocks before each test
  jest.clearAllMocks();
});

/**
 * Tests for FriendRoutes with mocking external dependencies
 * These tests use mocked database interactions
 */
describe('FriendRoutes API - With Mocks', () => {

  /**
   * GET /friends/:userID - Get User's Friends
   * Tests the route for retrieving a user's friends
   */
  describe('GET /friends/:userID - Get Friends', () => {
    
    /**
     * Test: Successfully get friends for user
     * Input: Valid userID
     * Expected Status: 200
     * Expected Output: Array of friends
     * Mock Behavior: findOne returns user with friends, then findOne for each friend returns friend data
     */
    test('should return friends for user', async () => {
      // Mock user with friends
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [TEST_FRIEND_ID]
      };
      
      // Mock friend data
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        photoURL: "https://example.com/friend.jpg",
        fcmToken: "test-friend-fcm-token",
        friends: [TEST_USER_ID],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call returns user
        .mockResolvedValueOnce(mockFriend); // Second call returns friend
      
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBe(1);
      
      // Check that friends array and friendRequests are stripped out
      expect(response.body[0].userID).toBe(TEST_FRIEND_ID);
      expect(response.body[0].displayName).toBe("Test Friend");
      expect(response.body[0].friends).toBeUndefined();
      expect(response.body[0].friendRequests).toBeUndefined();
      
      // Verify findOne was called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: TEST_USER_ID });
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: TEST_FRIEND_ID });
    });
    
    /**
     * Test: User with no friends
     * Input: Valid userID with empty friends array
     * Expected Status: 200
     * Expected Output: Empty array
     * Mock Behavior: findOne returns user with empty friends array
     */
    test('should return empty array for user with no friends', async () => {
      // Mock user with no friends
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
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
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .get(`/friends/non-existent-user`);
      
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
     * Mock Behavior: findOne returns user and friend, updateOne succeeds
     */
    test('should send friend request successfully', async () => {
      // Mock user
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: []
      };
      
      // Mock friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        email: TEST_FRIEND_EMAIL,
        displayName: "Test Friend",
        friends: [],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      // Mock updateOne to succeed
      (mockCollection.updateOne as jest.Mock).mockResolvedValueOnce({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request sent successfully');
      
      // Verify updateOne was called correctly
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_FRIEND_ID },
        { $push: { friendRequests: TEST_USER_ID } }
      );
    });
    
    /**
     * Test: Trying to send friend request to yourself
     * Input: UserID and own email
     * Expected Status: 400
     * Expected Output: Error message
     * Mock Behavior: findOne returns user with matching email
     */
    test('should return 400 when sending request to yourself', async () => {
      // Mock user
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        email: testUserData.email
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${testUserData.email}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('Cannot send friend request to yourself');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Already friends
     * Input: Valid userID and friendEmail of existing friend
     * Expected Status: 400
     * Expected Output: Already friends message
     * Mock Behavior: findOne returns user and friend, but friend is already in user's friends array
     */
    test('should return 400 when already friends', async () => {
      // Mock user with friend already in friends array
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [TEST_FRIEND_ID]
      };
      
      // Mock friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        email: TEST_FRIEND_EMAIL,
        displayName: "Test Friend",
        friends: [TEST_USER_ID],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('You are already friends');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Request already sent
     * Input: Valid userID and friendEmail for request already sent
     * Expected Status: 200
     * Expected Output: Already sent message
     * Mock Behavior: findOne returns user and friend, friend already has user in friendRequests
     */
    test('should return 200 when request already sent', async () => {
      // Mock user
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: []
      };
      
      // Mock friend with user already in friendRequests
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        email: TEST_FRIEND_EMAIL,
        displayName: "Test Friend",
        friends: [],
        friendRequests: [TEST_USER_ID]
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/${TEST_FRIEND_EMAIL}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('You have already sent a friend request to this user');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Friend not found
     * Input: Valid userID but non-existent friendEmail
     * Expected Status: 404
     * Expected Output: Friend not found message
     * Mock Behavior: findOne returns user, then null for friend
     */
    test('should return 404 for non-existent friend', async () => {
      // Mock user
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(null); // Second call for friend (not found)
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/sendRequest/nonexistent@example.com`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('Friend not found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
  });

  /**
   * GET /friends/:userID/friendRequests - Get Friend Requests
   * Tests the route for retrieving a user's friend requests
   */
  describe('GET /friends/:userID/friendRequests - Get Friend Requests', () => {
    
    /**
     * Test: Successfully get friend requests
     * Input: Valid userID with friend requests
     * Expected Status: 200
     * Expected Output: Array of friend request objects
     * Mock Behavior: findOne returns user with friendRequests, then findOne for each requester
     */
    test('should return friend requests for user', async () => {
      // Mock user with friend requests
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friendRequests: [TEST_FRIEND_ID]
      };
      
      // Mock friend who sent request
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        photoURL: "https://example.com/friend.jpg",
        friends: [],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for requester
      
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}/friendRequests`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBe(1);
      
      // Check that friends and friendRequests arrays are stripped out
      expect(response.body[0].userID).toBe(TEST_FRIEND_ID);
      expect(response.body[0].displayName).toBe("Test Friend");
      expect(response.body[0].friends).toBeUndefined();
      expect(response.body[0].friendRequests).toBeUndefined();
    });
    
    /**
     * Test: User with no friend requests
     * Input: Valid userID with empty friendRequests array
     * Expected Status: 200
     * Expected Output: Empty array
     * Mock Behavior: findOne returns user with empty friendRequests array
     */
    test('should return empty array for user with no friend requests', async () => {
      // Mock user with no friend requests
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      const response = await createTestRequest(app)
        .get(`/friends/${TEST_USER_ID}/friendRequests`);
      
      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body.length).toBe(0);
    });
    
    /**
     * Test: Try to get friend requests for non-existent user
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .get(`/friends/non-existent-user/friendRequests`);
      
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
     * Input: Valid userID and friendID
     * Expected Status: 200
     * Expected Output: Success message
     * Mock Behavior: findOne returns user and friend, updateOne succeeds for all operations
     */
    test('should accept friend request successfully', async () => {
      // Mock user with friend request
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: [TEST_FRIEND_ID]
      };
      
      // Mock friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        friends: [],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      // Mock updateOne to succeed for all operations
      (mockCollection.updateOne as jest.Mock).mockResolvedValue({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/acceptRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request responded to successfully');
      
      // Verify updateOne was called for adding friend to both users' friends lists
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_USER_ID },
        { $push: { friends: TEST_FRIEND_ID } }
      );
      
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_FRIEND_ID },
        { $push: { friends: TEST_USER_ID } }
      );
      
      // Verify updateOne was called for removing friend request
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_USER_ID },
        { $pull: { friendRequests: TEST_FRIEND_ID } }
      );
    });
    
    /**
     * Test: No friend request found
     * Input: Valid userID and friendID but no request exists
     * Expected Status: 400
     * Expected Output: No friend request found message
     * Mock Behavior: findOne returns user and friend, but friendRequests doesn't include friendID
     */
    test('should return 400 for non-existent friend request', async () => {
      // Mock user with no friend request from this friend
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: []
      };
      
      // Mock friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        friends: [],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/acceptRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(400);
      expect(response.text).toBe('No friend request found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: User or friend not found
     * Input: Invalid userID or friendID
     * Expected Status: 404
     * Expected Output: User or friend not found message
     * Mock Behavior: findOne returns null for either user or friend
     */
    test('should return 404 for non-existent user or friend', async () => {
      // Setup mock behavior - user not found
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .post(`/friends/non-existent-user/acceptRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User or friend not found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });

    /**
     * Test: Database error when accepting friend request
     * Input: Valid userID and friendID
     * Expected Status: 404
     * Expected Output: Error message
     * Mock Behavior: findOne succeeds but updateOne throws an error
     */
    test('should handle database error when accepting friend request', async () => {
      // Mock user with friend request
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: [TEST_FRIEND_ID]
      };
      
      // Mock friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        friends: [],
        friendRequests: []
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      // Mock updateOne to throw an error - this will trigger the catch block at line 84
      (mockCollection.updateOne as jest.Mock).mockRejectedValueOnce(new Error('Database error'));
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/acceptRequest/${TEST_FRIEND_ID}`);
      
      // This should hit line 84 in FriendController.ts
      expect(response.status).toBe(404);
      expect(response.text).toBe('Error responding to friend request');
      
      // Verify updateOne was called with the correct parameters
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_USER_ID },
        { $push: { friends: TEST_FRIEND_ID } }
      );
    });
  });

  /**
   * POST /friends/:userID/declineRequest/:friendID - Decline Friend Request
   * Tests the route for declining a friend request
   */
  describe('POST /friends/:userID/declineRequest/:friendID - Decline Friend Request', () => {
    
    /**
     * Test: Successfully decline friend request
     * Input: Valid userID and friendID
     * Expected Status: 200
     * Expected Output: Success message
     * Mock Behavior: findOne returns user, updateOne succeeds
     */
    test('should decline friend request successfully', async () => {
      // Mock user with friend request
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: [TEST_FRIEND_ID]
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      // Mock updateOne to succeed
      (mockCollection.updateOne as jest.Mock).mockResolvedValueOnce({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/declineRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend request declined successfully');
      
      // Verify updateOne was called correctly
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_USER_ID },
        { $pull: { friendRequests: TEST_FRIEND_ID } }
      );
    });
    
    /**
     * Test: User not found
     * Input: Invalid userID
     * Expected Status: 404
     * Expected Output: User not found message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .post(`/friends/non-existent-user/declineRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });

    /**
     * Test: Database error when declining friend request
     * Input: Valid userID and friendID
     * Expected Status: 404
     * Expected Output: Error message
     * Mock Behavior: findOne succeeds but updateOne throws an error
     */
    test('should handle database error when declining friend request', async () => {
      // Mock user with friend request
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [],
        friendRequests: [TEST_FRIEND_ID]
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      
      // Mock updateOne to throw an error
      (mockCollection.updateOne as jest.Mock).mockRejectedValueOnce(new Error('Database error'));
      
      const response = await createTestRequest(app)
        .post(`/friends/${TEST_USER_ID}/declineRequest/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('Error declining friend request');
    });
  });

  /**
   * PUT /friends/:userID/deleteFriend/:friendID - Delete Friend
   * Tests the route for deleting a friend
   */
  describe('PUT /friends/:userID/deleteFriend/:friendID - Delete Friend', () => {
    
    /**
     * Test: Successfully delete friend
     * Input: Valid userID and friendID
     * Expected Status: 200
     * Expected Output: Success message
     * Mock Behavior: findOne returns user and friend, updateOne succeeds for both operations
     */
    test('should delete friend successfully', async () => {
      // Mock user with friend
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [TEST_FRIEND_ID]
      };
      
      // Mock friend with user as friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        friends: [TEST_USER_ID]
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      // Mock updateOne to succeed for both operations
      (mockCollection.updateOne as jest.Mock).mockResolvedValue({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .put(`/friends/${TEST_USER_ID}/deleteFriend/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Friend deleted successfully');
      
      // Verify updateOne was called for removing from both users' friends lists
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_USER_ID },
        { $pull: { friends: TEST_FRIEND_ID } }
      );
      
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: TEST_FRIEND_ID },
        { $pull: { friends: TEST_USER_ID } }
      );
    });
    
    /**
     * Test: User or friend not found
     * Input: Invalid userID or friendID
     * Expected Status: 404
     * Expected Output: User or friend not found message
     * Mock Behavior: findOne returns null for either user or friend
     */
    test('should return 404 for non-existent user or friend', async () => {
      // Setup mock behavior - user not found
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .put(`/friends/non-existent-user/deleteFriend/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User or friend not found');
      
      // Verify updateOne was not called
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });

    /**
     * Test: Database error when deleting friend
     * Input: Valid userID and friendID
     * Expected Status: 404
     * Expected Output: Error message
     * Mock Behavior: findOne succeeds but updateOne throws an error
     */
    test('should handle database error when deleting friend', async () => {
      // Mock user with friend
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        friends: [TEST_FRIEND_ID]
      };
      
      // Mock friend with user as friend
      const mockFriend = {
        _id: new ObjectId(),
        userID: TEST_FRIEND_ID,
        displayName: "Test Friend",
        email: TEST_FRIEND_EMAIL,
        friends: [TEST_USER_ID]
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock)
        .mockResolvedValueOnce(mockUser) // First call for user
        .mockResolvedValueOnce(mockFriend); // Second call for friend
      
      // Mock updateOne to throw an error
      (mockCollection.updateOne as jest.Mock).mockRejectedValueOnce(new Error('Database error'));
      
      const response = await createTestRequest(app)
        .put(`/friends/${TEST_USER_ID}/deleteFriend/${TEST_FRIEND_ID}`);
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('Error deleting friend');
    });
  });
}); 