export interface Point {
  x: number;
  y: number;
  data: any;
}

export interface Rectangle {
  x: number;
  y: number;
  width: number;
  height: number;
}

export class Quadtree {
  private capacity: number;
  private boundary: Rectangle;
  private points: Point[] = [];
  private divided: boolean = false;
  private northwest?: Quadtree;
  private northeast?: Quadtree;
  private southwest?: Quadtree;
  private southeast?: Quadtree;

  constructor(boundary: Rectangle, capacity: number = 4) {
    this.boundary = boundary;
    this.capacity = capacity;
  }

  // Check if the point is in the boundary
  private contains(point: Point): boolean {
    return (
      point.x >= this.boundary.x - this.boundary.width &&
      point.x <= this.boundary.x + this.boundary.width &&
      point.y >= this.boundary.y - this.boundary.height &&
      point.y <= this.boundary.y + this.boundary.height
    );
  }

  // Subdivide the quadtree into four quadrants
  private subdivide(): void {
    const x = this.boundary.x;
    const y = this.boundary.y;
    const w = this.boundary.width / 2;
    const h = this.boundary.height / 2;

    const nw = { x: x - w, y: y - h, width: w, height: h };
    const ne = { x: x + w, y: y - h, width: w, height: h };
    const sw = { x: x - w, y: y + h, width: w, height: h };
    const se = { x: x + w, y: y + h, width: w, height: h };

    this.northwest = new Quadtree(nw, this.capacity);
    this.northeast = new Quadtree(ne, this.capacity);
    this.southwest = new Quadtree(sw, this.capacity);
    this.southeast = new Quadtree(se, this.capacity);

    this.divided = true;
  }

  // Insert a point into the quadtree
  insert(point: Point): boolean {
    if (!this.contains(point)) {
      return false;
    }

    if (this.points.length < this.capacity) {
      this.points.push(point);
      return true;
    }

    if (!this.divided) {
      this.subdivide();
    }

    return (
      this.northwest!.insert(point) ||
      this.northeast!.insert(point) ||
      this.southwest!.insert(point) ||
      this.southeast!.insert(point)
    );
  }

  // Query points within a range
  query(range: Rectangle, found: Point[] = []): Point[] {
    // If the range doesn't intersect this quad, return empty array
    if (
      range.x - range.width > this.boundary.x + this.boundary.width ||
      range.x + range.width < this.boundary.x - this.boundary.width ||
      range.y - range.height > this.boundary.y + this.boundary.height ||
      range.y + range.height < this.boundary.y - this.boundary.height
    ) {
      return found;
    }

    // Check points in this quad
    for (const point of this.points) {
      if (
        point.x >= range.x - range.width &&
        point.x <= range.x + range.width &&
        point.y >= range.y - range.height &&
        point.y <= range.y + range.height
      ) {
        found.push(point);
      }
    }

    // If this quad is divided, check children
    if (this.divided) {
      this.northwest!.query(range, found);
      this.northeast!.query(range, found);
      this.southwest!.query(range, found);
      this.southeast!.query(range, found);
    }

    return found;
  }
} 