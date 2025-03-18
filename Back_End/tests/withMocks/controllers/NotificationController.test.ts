import { NotificationController } from '../../../controllers/NotificationController';
import { User } from '../../../types/user.types';
import { Quadtree } from '../../../utils/Quadtree';

// Mock the quadtree module
jest.mock('../../../utils/Quadtree', () => {
  // Create mock implementation of Quadtree class
  const mockInsert = jest.fn().mockReturnValue(true);
  const mockQuery = jest.fn().mockReturnValue([]);
  const mockContains = jest.fn().mockReturnValue(true);
  const mockSubdivide = jest.fn();

  return {
    Quadtree: jest.fn().mockImplementation(() => ({
      insert: mockInsert,
      query: mockQuery,
      contains: mockContains,
      subdivide: mockSubdivide,
      // Add required properties to match Quadtree interface
      capacity: 4,
      boundary: { x: 0, y: 0, width: 180, height: 90 },
      points: [],
      divided: false,
      northwest: undefined,
      northeast: undefined,
      southwest: undefined,
      southeast: undefined
    })),
    // We need to export the Point and Rectangle interfaces too
    Point: jest.requireActual('../../../utils/Quadtree').Point,
    Rectangle: jest.requireActual('../../../utils/Quadtree').Rectangle
  };
});

// Directly import the mocked Quadtree
const mockedQuadtree = jest.mocked(Quadtree);

describe('NotificationController - Quadtree Implementation', () => {
  // Reset all mocks before each test
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('findNearbyFriendsWithQuadtree', () => {
    // Test for line 48: quadtree initialization
    test('should initialize quadtree with world boundary', () => {
      // Mock user and friends data
      const mockUser = {
        userID: 'user123',
        currentLocation: {
          latitude: 49.2827,
          longitude: -123.1207
        }
      };
      const mockFriends: User[] = [];

      // Get the private method via type casting and reflection
      const findNearbyFriendsWithQuadtree = 
        (NotificationController as any).findNearbyFriendsWithQuadtree;
      
      // Call the method
      findNearbyFriendsWithQuadtree(mockUser, mockFriends);

      // Verify Quadtree was initialized with correct parameters
      expect(mockedQuadtree).toHaveBeenCalledWith({
        x: 0,      // center longitude
        y: 0,      // center latitude
        width: 180, // half the longitude range
        height: 90  // half the latitude range
      });
    });

    // Test for lines 105-114: inserting friends into quadtree
    test('should insert friends with location into quadtree', () => {
      // Mock user and friends data
      const mockUser = {
        userID: 'user123',
        currentLocation: {
          latitude: 49.2827,
          longitude: -123.1207
        }
      };
      const mockFriends = [
        {
          userID: 'friend1',
          currentLocation: {
            latitude: 49.2828,
            longitude: -123.1208
          }
        },
        {
          userID: 'friend2',
          currentLocation: {
            latitude: 49.2829,
            longitude: -123.1209
          }
        },
        {
          userID: 'friend3',
          // Friend without location
        }
      ];

      // Get the private method via type casting and reflection
      const findNearbyFriendsWithQuadtree = 
        (NotificationController as any)['findNearbyFriendsWithQuadtree'];
      
      // Call the method
      findNearbyFriendsWithQuadtree(mockUser, mockFriends);

      // Get the mock instance
      const mockQuadtreeInstance = mockedQuadtree.mock.results[0].value;
      
      // Verify insert was called for each friend with location
      expect(mockQuadtreeInstance.insert).toHaveBeenCalledTimes(2); // Only 2 friends have location
      
      // Use proper null checks instead of non-null assertions
      const friend0Location = mockFriends[0].currentLocation;
      const friend1Location = mockFriends[1].currentLocation;
      
      // Make sure location exists before checking
      if (friend0Location) {
        expect(mockQuadtreeInstance.insert).toHaveBeenCalledWith({
          x: friend0Location.longitude,
          y: friend0Location.latitude,
          data: mockFriends[0]
        });
      }
      
      if (friend1Location) {
        expect(mockQuadtreeInstance.insert).toHaveBeenCalledWith({
          x: friend1Location.longitude,
          y: friend1Location.latitude,
          data: mockFriends[1]
        });
      }
    });

    // Test for lines 115-136: calculating search range and querying
    test('should calculate search range and query quadtree', () => {
      // Mock user data
      const mockUser = {
        userID: 'user123',
        currentLocation: {
          latitude: 49.2827,
          longitude: -123.1207
        }
      };
      
      // Mock friends data (not important for this test)
      const mockFriends = [
        {
          userID: 'friend1',
          currentLocation: {
            latitude: 49.2828,
            longitude: -123.1208
          }
        }
      ];

      // Calculate expected search range
      // Expected values based on code logic:
      // latDegree = 1 / 111 ≈ 0.009
      // lonDegree = 1 / (111 * Math.cos(49.2827 * Math.PI / 180)) ≈ 0.014
      const latDegree = 1 / 111; 
      const lonDegree = 1 / (111 * Math.cos(mockUser.currentLocation.latitude * Math.PI / 180));
      const expectedSearchRange = {
        x: mockUser.currentLocation.longitude,
        y: mockUser.currentLocation.latitude,
        width: lonDegree,
        height: latDegree
      };

      // Get the private method via type casting and reflection
      const findNearbyFriendsWithQuadtree = 
        (NotificationController as any)['findNearbyFriendsWithQuadtree'];
      
      // Call the method
      findNearbyFriendsWithQuadtree(mockUser, mockFriends);

      // Get the mock instance
      const mockQuadtreeInstance = mockedQuadtree.mock.results[0].value;
      
      // Verify query was called with correct search range
      expect(mockQuadtreeInstance.query).toHaveBeenCalledTimes(1);
      
      // Test the range values with approximate equality
      const actualRange = mockQuadtreeInstance.query.mock.calls[0][0];
      expect(actualRange.x).toBeCloseTo(expectedSearchRange.x);
      expect(actualRange.y).toBeCloseTo(expectedSearchRange.y);
      expect(actualRange.width).toBeCloseTo(expectedSearchRange.width);
      expect(actualRange.height).toBeCloseTo(expectedSearchRange.height);
    });

    // Test for line 130-136: returning nearby friends
    test('should return nearby friends from query results', () => {
      // Mock user data
      const mockUser = {
        userID: 'user123',
        currentLocation: {
          latitude: 49.2827,
          longitude: -123.1207
        }
      };
      
      // Mock friends data
      const mockFriends = [
        {
          userID: 'friend1',
          currentLocation: {
            latitude: 49.2828,
            longitude: -123.1208
          }
        }
      ];

      // Mock query results
      const nearbyPoints = [
        {
          x: -123.1208,
          y: 49.2828,
          data: mockFriends[0]
        }
      ];

      // Create a mock of Quadtree for this test
      const mockContains = jest.fn().mockReturnValue(true);
      const mockSubdivide = jest.fn();
      
      const mockQuadtreeInstance = {
        insert: jest.fn().mockReturnValue(true),
        query: jest.fn().mockReturnValue(nearbyPoints),
        contains: mockContains,
        subdivide: mockSubdivide,
        // Add required properties to match Quadtree interface
        capacity: 4,
        boundary: { x: 0, y: 0, width: 180, height: 90 },
        points: [],
        divided: false,
        northwest: undefined,
        northeast: undefined,
        southwest: undefined,
        southeast: undefined
      };
      
      mockedQuadtree.mockImplementationOnce(() => mockQuadtreeInstance as unknown as Quadtree);

      // Get the private method via type casting and reflection
      const findNearbyFriendsWithQuadtree = 
        (NotificationController as any)['findNearbyFriendsWithQuadtree'];
      
      // Call the method and get result
      const result = findNearbyFriendsWithQuadtree(mockUser, mockFriends);

      // Verify result contains the nearby friends
      expect(result).toEqual([mockFriends[0]]);
      expect(result.length).toBe(1);
    });

    // Specific test for line 136: Verifying the mapping of point data to friend data
    test('should correctly map and return points data as nearby friends (line 136)', () => {
      // Mock user data
      const mockUser = {
        userID: 'user123',
        currentLocation: {
          latitude: 49.2827,
          longitude: -123.1207
        }
      };
      
      // Create a more varied set of friends
      const mockFriends = [
        {
          userID: 'friend1',
          displayName: 'Friend One',
          currentLocation: {
            latitude: 49.2828,
            longitude: -123.1208
          }
        },
        {
          userID: 'friend2',
          displayName: 'Friend Two',
          currentLocation: {
            latitude: 49.2829,
            longitude: -123.1209
          }
        },
        {
          userID: 'friend3',
          displayName: 'Friend Three',
          currentLocation: {
            latitude: 49.2830,
            longitude: -123.1210
          }
        }
      ];

      // Mock different scenarios with multiple friends in range
      const nearbyPoints = [
        {
          x: -123.1208,
          y: 49.2828,
          data: mockFriends[0]  // first friend
        },
        {
          x: -123.1209,
          y: 49.2829,
          data: mockFriends[1]  // second friend
        },
        {
          x: -123.1210,
          y: 49.2830,
          data: mockFriends[2]  // third friend
        }
      ];

      // Create a mock of Quadtree for this test
      const mockQuadtreeInstance = {
        insert: jest.fn().mockReturnValue(true),
        query: jest.fn().mockReturnValue(nearbyPoints),
        contains: jest.fn().mockReturnValue(true),
        subdivide: jest.fn(),
        // Add required properties to match Quadtree interface
        capacity: 4,
        boundary: { x: 0, y: 0, width: 180, height: 90 },
        points: [],
        divided: false,
        northwest: undefined,
        northeast: undefined,
        southwest: undefined,
        southeast: undefined
      };
      
      mockedQuadtree.mockImplementationOnce(() => mockQuadtreeInstance as unknown as Quadtree);

      // Get the private method via type casting and reflection
      const findNearbyFriendsWithQuadtree = 
        (NotificationController as any)['findNearbyFriendsWithQuadtree'];
      
      // Call the method and get result
      const result = findNearbyFriendsWithQuadtree(mockUser, mockFriends);

      // Verify result contains ALL nearby friends in the same order as the points
      expect(result).toEqual([mockFriends[0], mockFriends[1], mockFriends[2]]);
      expect(result.length).toBe(3);
      
      // More specific checks to ensure the mapping works correctly
      expect(result[0].userID).toBe('friend1');
      expect(result[0].displayName).toBe('Friend One');
      expect(result[1].userID).toBe('friend2');
      expect(result[2].userID).toBe('friend3');
      
      // Also test with empty results to ensure line 136 still works
      const emptyQuadtreeInstance = {
        ...mockQuadtreeInstance,
        query: jest.fn().mockReturnValue([])
      };
      
      mockedQuadtree.mockImplementationOnce(() => emptyQuadtreeInstance as unknown as Quadtree);
      
      // Call again with empty results
      const emptyResult = findNearbyFriendsWithQuadtree(mockUser, mockFriends);
      
      // Verify empty array is returned when no nearby friends
      expect(emptyResult).toEqual([]);
      expect(emptyResult.length).toBe(0);
    });
  });
}); 