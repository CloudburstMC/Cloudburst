package org.cloudburstmc.server.level.collision;

import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class CloudVoxelShape implements VoxelShape {

    static final int MAX_BOXES = 64;

    private final float[] boxes;

    public CloudVoxelShape(float[] boxes) {
        this.boxes = normalize(boxes);
    }

    private static float[] normalize(float[] boxes) {
        Objects.requireNonNull(boxes, "boxes");
        if (boxes.length % 6 != 0) {
            throw new IllegalArgumentException("Voxel shape box data must be a multiple of 6");
        }

        if (boxes.length / 6 > MAX_BOXES) {
            throw new IllegalArgumentException("Voxel shapes may contain at most " + MAX_BOXES + " boxes");
        }

        float[] validBoxes = new float[boxes.length];
        int validLength = 0;
        for (int i = 0; i < boxes.length; i += 6) {
            float minX = boxes[i];
            float minY = boxes[i + 1];
            float minZ = boxes[i + 2];
            float maxX = boxes[i + 3];
            float maxY = boxes[i + 4];
            float maxZ = boxes[i + 5];

            if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                    || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) {
                throw new IllegalArgumentException("Shape bounds must be finite");
            }

            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException("Shape minimum bounds must be less than or equal to maximum bounds");
            }

            if (maxX == minX || maxY == minY || maxZ == minZ) {
                continue;
            }

            validBoxes[validLength++] = minX;
            validBoxes[validLength++] = minY;
            validBoxes[validLength++] = minZ;
            validBoxes[validLength++] = maxX;
            validBoxes[validLength++] = maxY;
            validBoxes[validLength++] = maxZ;
        }

        return validLength == boxes.length ? Arrays.copyOf(boxes, boxes.length) : Arrays.copyOf(validBoxes, validLength);
    }

    @Override
    public boolean isEmpty() {
        return this.boxes.length == 0;
    }

    @Override
    public BoundingBox bounds() {
        if (this.isEmpty()) {
            throw new UnsupportedOperationException("Empty shapes do not have bounds");
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < this.boxes.length; i += 6) {
            minX = Math.min(minX, this.boxes[i]);
            minY = Math.min(minY, this.boxes[i + 1]);
            minZ = Math.min(minZ, this.boxes[i + 2]);
            maxX = Math.max(maxX, this.boxes[i + 3]);
            maxY = Math.max(maxY, this.boxes[i + 4]);
            maxZ = Math.max(maxZ, this.boxes[i + 5]);
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public List<BoundingBox> getBoundingBoxes() {
        List<BoundingBox> result = new ArrayList<>(this.boxes.length / 6);
        this.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                result.add(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)));
        return List.copyOf(result);
    }

    @Override
    public VoxelShape move(float x, float y, float z) {
        if (this.isEmpty()) {
            return CloudVoxelShapes.empty();
        }

        float[] moved = Arrays.copyOf(this.boxes, this.boxes.length);
        for (int i = 0; i < moved.length; i += 6) {
            moved[i] += x;
            moved[i + 1] += y;
            moved[i + 2] += z;
            moved[i + 3] += x;
            moved[i + 4] += y;
            moved[i + 5] += z;
        }

        return new CloudVoxelShape(moved);
    }

    @Override
    public VoxelShape getFaceShape(Direction face) {
        if (this.isEmpty()) {
            return CloudVoxelShapes.empty();
        }

        float[] faceBoxes = new float[this.boxes.length];
        int length = 0;
        for (int i = 0; i < this.boxes.length; i += 6) {
            if (!touchesFace(face, i)) {
                continue;
            }

            System.arraycopy(this.boxes, i, faceBoxes, length, 6);
            switch (face.getAxis()) {
                case X -> {
                    faceBoxes[length] = 0;
                    faceBoxes[length + 3] = 1;
                }
                case Y -> {
                    faceBoxes[length + 1] = 0;
                    faceBoxes[length + 4] = 1;
                }
                case Z -> {
                    faceBoxes[length + 2] = 0;
                    faceBoxes[length + 5] = 1;
                }
            }
            length += 6;
        }

        return length == 0 ? CloudVoxelShapes.empty() : new CloudVoxelShape(Arrays.copyOf(faceBoxes, length));
    }

    @Override
    public boolean covers(VoxelShape required) {
        return CloudVoxelShapes.covers(this, required);
    }

    @Override
    public boolean overlaps(BoundingBox box) {
        return this.overlaps(box, 0, 0, 0);
    }

    @Override
    public boolean overlaps(BoundingBox box, float offsetX, float offsetY, float offsetZ) {
        for (int i = 0; i < this.boxes.length; i += 6) {
            if (this.overlaps(box, i, offsetX, offsetY, offsetZ)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public float collide(Direction.Axis axis, BoundingBox box, float movement, float offsetX, float offsetY, float offsetZ) {
        return switch (axis) {
            case X -> this.collideX(box, movement, offsetX, offsetY, offsetZ);
            case Y -> this.collideY(box, movement, offsetX, offsetY, offsetZ);
            case Z -> this.collideZ(box, movement, offsetX, offsetY, offsetZ);
        };
    }

    private float collideX(BoundingBox box, float movement, float offsetX, float offsetY, float offsetZ) {
        if (Math.abs(movement) < CloudVoxelShapes.EPSILON) {
            return 0;
        }

        for (int i = 0; i < this.boxes.length; i += 6) {
            if (box.getMaxY() <= this.boxes[i + 1] + offsetY || box.getMinY() >= this.boxes[i + 4] + offsetY) {
                continue;
            }

            if (box.getMaxZ() <= this.boxes[i + 2] + offsetZ || box.getMinZ() >= this.boxes[i + 5] + offsetZ) {
                continue;
            }

            if (movement > 0 && box.getMaxX() <= this.boxes[i] + offsetX) {
                float limit = this.boxes[i] + offsetX - box.getMaxX();
                if (limit >= -CloudVoxelShapes.EPSILON && limit < movement) {
                    movement = limit;
                }
            } else if (movement < 0 && box.getMinX() >= this.boxes[i + 3] + offsetX) {
                float limit = this.boxes[i + 3] + offsetX - box.getMinX();
                if (limit <= CloudVoxelShapes.EPSILON && limit > movement) {
                    movement = limit;
                }
            }
        }

        return movement;
    }

    private float collideY(BoundingBox box, float movement, float offsetX, float offsetY, float offsetZ) {
        if (Math.abs(movement) < CloudVoxelShapes.EPSILON) {
            return 0;
        }

        for (int i = 0; i < this.boxes.length; i += 6) {
            if (box.getMaxX() <= this.boxes[i] + offsetX || box.getMinX() >= this.boxes[i + 3] + offsetX) {
                continue;
            }

            if (box.getMaxZ() <= this.boxes[i + 2] + offsetZ || box.getMinZ() >= this.boxes[i + 5] + offsetZ) {
                continue;
            }

            if (movement > 0 && box.getMaxY() <= this.boxes[i + 1] + offsetY) {
                float limit = this.boxes[i + 1] + offsetY - box.getMaxY();
                if (limit >= -CloudVoxelShapes.EPSILON && limit < movement) {
                    movement = limit;
                }
            } else if (movement < 0 && box.getMinY() >= this.boxes[i + 4] + offsetY) {
                float limit = this.boxes[i + 4] + offsetY - box.getMinY();
                if (limit <= CloudVoxelShapes.EPSILON && limit > movement) {
                    movement = limit;
                }
            }
        }

        return movement;
    }

    private float collideZ(BoundingBox box, float movement, float offsetX, float offsetY, float offsetZ) {
        if (Math.abs(movement) < CloudVoxelShapes.EPSILON) {
            return 0;
        }

        for (int i = 0; i < this.boxes.length; i += 6) {
            if (box.getMaxX() <= this.boxes[i] + offsetX || box.getMinX() >= this.boxes[i + 3] + offsetX) {
                continue;
            }

            if (box.getMaxY() <= this.boxes[i + 1] + offsetY || box.getMinY() >= this.boxes[i + 4] + offsetY) {
                continue;
            }

            if (movement > 0 && box.getMaxZ() <= this.boxes[i + 2] + offsetZ) {
                float limit = this.boxes[i + 2] + offsetZ - box.getMaxZ();
                if (limit >= -CloudVoxelShapes.EPSILON && limit < movement) {
                    movement = limit;
                }
            } else if (movement < 0 && box.getMinZ() >= this.boxes[i + 5] + offsetZ) {
                float limit = this.boxes[i + 5] + offsetZ - box.getMinZ();
                if (limit <= CloudVoxelShapes.EPSILON && limit > movement) {
                    movement = limit;
                }
            }
        }

        return movement;
    }

    @Override
    public void forEachBox(BoxConsumer consumer) {
        this.forEachBox(0, 0, 0, consumer);
    }

    @Override
    public void forEachBox(float offsetX, float offsetY, float offsetZ, BoxConsumer consumer) {
        for (int i = 0; i < this.boxes.length; i += 6) {
            consumer.accept(
                    this.boxes[i] + offsetX, this.boxes[i + 1] + offsetY, this.boxes[i + 2] + offsetZ,
                    this.boxes[i + 3] + offsetX, this.boxes[i + 4] + offsetY, this.boxes[i + 5] + offsetZ
            );
        }
    }

    @Override
    public boolean anyBox(BoxPredicate predicate) {
        return this.anyBox(0, 0, 0, predicate);
    }

    @Override
    public boolean anyBox(float offsetX, float offsetY, float offsetZ, BoxPredicate predicate) {
        for (int i = 0; i < this.boxes.length; i += 6) {
            if (predicate.test(
                    this.boxes[i] + offsetX, this.boxes[i + 1] + offsetY, this.boxes[i + 2] + offsetZ,
                    this.boxes[i + 3] + offsetX, this.boxes[i + 4] + offsetY, this.boxes[i + 5] + offsetZ
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean overlaps(BoundingBox box, int index, float offsetX, float offsetY, float offsetZ) {
        return box.getMaxX() > this.boxes[index] + offsetX
                && box.getMinX() < this.boxes[index + 3] + offsetX
                && box.getMaxY() > this.boxes[index + 1] + offsetY
                && box.getMinY() < this.boxes[index + 4] + offsetY
                && box.getMaxZ() > this.boxes[index + 2] + offsetZ
                && box.getMinZ() < this.boxes[index + 5] + offsetZ;
    }

    private boolean touchesFace(Direction face, int index) {
        return switch (face) {
            case DOWN -> this.boxes[index + 1] <= CloudVoxelShapes.EPSILON;
            case UP -> this.boxes[index + 4] >= 1 - CloudVoxelShapes.EPSILON;
            case NORTH -> this.boxes[index + 2] <= CloudVoxelShapes.EPSILON;
            case SOUTH -> this.boxes[index + 5] >= 1 - CloudVoxelShapes.EPSILON;
            case WEST -> this.boxes[index] <= CloudVoxelShapes.EPSILON;
            case EAST -> this.boxes[index + 3] >= 1 - CloudVoxelShapes.EPSILON;
        };
    }
}
