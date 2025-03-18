import { Express } from 'express';
import { ObjectId } from 'mongodb';
import { setupTestApp, createTestRequest, testUserData } from '../../testSetup';
import '../../setupFirebaseMock'; // Import Firebase mocking
import { client } from '../../../services';

// Set a long timeout for the entire test suite
jest.setTimeout(3000);

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
 * Tests for LocationRoutes with mocking external dependencies
 * These tests use mocked database interactions
 */
describe('LocationRoutes API - With Mocks', () => {

  /**
   * PUT /location/:userID - Update User Location
   * Tests the route for updating a user's location
   */
  describe('PUT /location/:userID - Update User Location', () => {
    
    /**
     * Test: Successfully update user location
     * Input: Valid userID and location data
     * Expected Status: 200
     * Expected Output: "Location updated" message
     * Mock Behavior: findOne returns user, updateOne succeeds
     */
    test('should update location for existing user', async () => {
      // Mock user
      const mockUser = {
        ...testUserData,
        _id: new ObjectId(),
        currentLocation: {
          latitude: 49.0000,
          longitude: -123.0000,
          timestamp: Date.now() - 10000 // Old timestamp
        }
      };
      
      // New location to set
      const newLocation = {
        latitude: 49.2827,
        longitude: -123.1207,
        timestamp: Date.now()
      };
      
      // Setup mock behavior
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(mockUser);
      (mockCollection.updateOne as jest.Mock).mockResolvedValueOnce({
        matchedCount: 1,
        modifiedCount: 1,
        acknowledged: true
      });
      
      const response = await createTestRequest(app)
        .put(`/location/${testUserData.userID}`)
        .send({ currentLocation: newLocation });
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Location updated');
      
      // Verify findOne and updateOne were called correctly
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: testUserData.userID });
      expect(mockCollection.updateOne).toHaveBeenCalledWith(
        { userID: testUserData.userID },
        { $set: { currentLocation: newLocation } }
      );
    });
    
    /**
     * Test: Try to update location for non-existent user
     * Input: Invalid userID and valid location data
     * Expected Status: 404
     * Expected Output: "User not found" message
     * Mock Behavior: findOne returns null
     */
    test('should return 404 for non-existent user', async () => {
      // New location to set
      const newLocation = {
        latitude: 49.2827,
        longitude: -123.1207,
        timestamp: Date.now()
      };
      
      // Setup mock behavior - user not found
      (mockCollection.findOne as jest.Mock).mockResolvedValueOnce(null);
      
      const response = await createTestRequest(app)
        .put('/location/non-existent-user')
        .send({ currentLocation: newLocation });
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
      
      // Verify findOne was called but updateOne was not
      expect(mockCollection.findOne).toHaveBeenCalledWith({ userID: 'non-existent-user' });
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Try to update location with missing location data
     * Input: Valid userID but missing location data
     * Expected Status: 400
     * Expected Output: Validation errors
     * Mock Behavior: No database calls should occur
     */
    test('should return validation errors for missing location data', async () => {
      const response = await createTestRequest(app)
        .put(`/location/${testUserData.userID}`)
        .send({}); // No location data
      
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('errors');
      expect(Array.isArray(response.body.errors)).toBe(true);
      
      // Verify no database calls were made
      expect(mockCollection.findOne).not.toHaveBeenCalled();
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
    
    /**
     * Test: Try to update location with invalid location data format
     * Input: Valid userID but invalid location format
     * Expected Status: 400
     * Expected Output: Validation errors
     * Mock Behavior: No database calls should occur
     */
    test('should return validation errors for invalid location format', async () => {
      const response = await createTestRequest(app)
        .put(`/location/${testUserData.userID}`)
        .send({ currentLocation: 'not an object' }); // Invalid location format
      
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('errors');
      expect(Array.isArray(response.body.errors)).toBe(true);
      
      // Verify no database calls were made
      expect(mockCollection.findOne).not.toHaveBeenCalled();
      expect(mockCollection.updateOne).not.toHaveBeenCalled();
    });
  });
}); 