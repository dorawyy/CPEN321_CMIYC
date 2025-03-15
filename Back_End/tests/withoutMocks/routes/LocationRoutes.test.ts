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
    
    // Create a test user for location updates if needed
    const existingUser = await client.db("cmiyc").collection("users").findOne({ userID: testUserData.userID });
    if (!existingUser) {
      await client.db("cmiyc").collection("users").insertOne({
        ...testUserData,
        friends: [],
        friendRequests: [],
        logList: []
      });
    }
  } catch (error) {
    console.error('Error connecting to database:', error);
    throw error;
  }
});

afterAll(async () => {
  // Clean up test data
  try {
    await client.db("cmiyc").collection("users").deleteOne({ userID: testUserData.userID });
    await client.close();
  } catch (error) {
    console.error('Error during cleanup:', error);
  }
});

/**
 * Tests for LocationRoutes API without mocking
 * These tests use the actual database connection
 */
describe('LocationRoutes API - No Mocks', () => {
  
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
     * Expected Behavior: The location should be updated in the database
     */
    test('should update location for existing user', async () => {
      const testLocation = {
        latitude: 49.2827,
        longitude: -123.1207,
        timestamp: Date.now()
      };

      const response = await createTestRequest(app)
        .put(`/location/${testUserData.userID}`)
        .send({ currentLocation: testLocation });
      
      expect(response.status).toBe(200);
      expect(response.text).toBe('Location updated');
      
      // Verify the location was updated
      const updatedUser = await client.db("cmiyc").collection("users").findOne({ userID: testUserData.userID });
      expect(updatedUser).not.toBeNull();
      expect(updatedUser?.currentLocation).toEqual(testLocation);
    });
    
    /**
     * Test: Try to update location for non-existent user
     * Input: Invalid userID and valid location data
     * Expected Status: 404
     * Expected Output: "User not found" message
     * Expected Behavior: No location should be updated
     */
    test('should return 404 for non-existent user', async () => {
      const testLocation = {
        latitude: 49.2827,
        longitude: -123.1207,
        timestamp: Date.now()
      };

      const response = await createTestRequest(app)
        .put('/location/non-existent-user')
        .send({ currentLocation: testLocation });
      
      expect(response.status).toBe(404);
      expect(response.text).toBe('User not found');
    });
    
    /**
     * Test: Try to update location with missing location data
     * Input: Valid userID but missing location data
     * Expected Status: 400
     * Expected Output: Validation errors
     * Expected Behavior: No location should be updated
     */
    test('should return validation errors for missing location data', async () => {
      const response = await createTestRequest(app)
        .put(`/location/${testUserData.userID}`)
        .send({}); // No location data
      
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('errors');
      expect(Array.isArray(response.body.errors)).toBe(true);
    });
    
    /**
     * Test: Try to update location with invalid location data format
     * Input: Valid userID but invalid location format
     * Expected Status: 400
     * Expected Output: Validation errors
     * Expected Behavior: No location should be updated
     */
    test('should return validation errors for invalid location format', async () => {
      const response = await createTestRequest(app)
        .put(`/location/${testUserData.userID}`)
        .send({ currentLocation: 'not an object' }); // Invalid location format
      
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('errors');
      expect(Array.isArray(response.body.errors)).toBe(true);
    });
  });
}); 