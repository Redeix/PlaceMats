package net.placemats.client.util;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RotationHelper {

    public record Rotations(float pitch, float roll, float yaw) {
        public float rotation() {
            return yaw;
        }
    }

    private RotationHelper() {}

    /**
     * Converts pitch, roll, and yaw angles (in degrees) to a Quaternionf orientation.
     */
    public static Quaternionf toQuaternion(float pitch, float roll, float yaw) {
        return new Quaternionf()
                .rotationX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll))
                .rotateY((float) Math.toRadians(yaw));
    }

    /**
     * Converts a Quaternionf orientation into Euler angles (pitch, roll, yaw in degrees).
     */
    public static Rotations fromQuaternion(Quaternionf q) {
        Vector3f vx = q.transform(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f());
        Vector3f vy = q.transform(new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f());
        Vector3f vz = q.transform(new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f());

        float sinRoll = Math.max(-1.0f, Math.min(1.0f, -vy.x));
        float rollRad = (float) Math.asin(sinRoll);

        float pitchRad;
        float yawRad;

        if (Math.abs(sinRoll) < 0.9999f) {
            pitchRad = (float) Math.atan2(vy.z, vy.y);
            yawRad = (float) Math.atan2(vz.x, vx.x);
        } else {
            // Lock
            yawRad = 0.0f;
            if (sinRoll > 0.0f) {
                pitchRad = (float) Math.atan2(vx.z, vx.y);
            } else {
                pitchRad = (float) Math.atan2(-vx.z, vz.z);
            }
        }

        float pitch = normalizeDegrees((float) Math.toDegrees(pitchRad));
        float roll = normalizeDegrees((float) Math.toDegrees(rollRad));
        float yaw = normalizeDegrees((float) Math.toDegrees(yawRad));

        return new Rotations(pitch, roll, yaw);
    }

    /**
     * Applies a rotation around the fixed world X-axis.
     */
    public static Rotations rotateX(float pitch, float roll, float yaw, float deltaDegrees) {
        Quaternionf current = toQuaternion(pitch, roll, yaw);
        Quaternionf delta = new Quaternionf().rotationX((float) Math.toRadians(deltaDegrees));
        Quaternionf result = delta.mul(current);
        return fromQuaternion(result);
    }

    /**
     * Applies a rotation around the fixed world Y-axis.
     */
    public static Rotations rotateY(float pitch, float roll, float yaw, float deltaDegrees) {
        Quaternionf current = toQuaternion(pitch, roll, yaw);
        Quaternionf delta = new Quaternionf().rotationY((float) Math.toRadians(deltaDegrees));
        Quaternionf result = delta.mul(current);
        return fromQuaternion(result);
    }

    /**
     * Applies a rotation around the fixed world Z-axis.
     */
    public static Rotations rotateZ(float pitch, float roll, float yaw, float deltaDegrees) {
        Quaternionf current = toQuaternion(pitch, roll, yaw);
        Quaternionf delta = new Quaternionf().rotationZ((float) Math.toRadians(deltaDegrees));
        Quaternionf result = delta.mul(current);
        return fromQuaternion(result);
    }

    /**
     * Snaps Euler angles (pitch, roll, yaw) to the nearest step increment in degrees (ex. 15, 45, 90).
     */
    public static Rotations snap(float pitch, float roll, float yaw, float stepDegrees) {
        if (stepDegrees <= 0.0f) {
            return new Rotations(pitch, roll, yaw);
        }
        float p = normalizeDegrees(Math.round(pitch / stepDegrees) * stepDegrees);
        float r = normalizeDegrees(Math.round(roll / stepDegrees) * stepDegrees);
        float y = normalizeDegrees(Math.round(yaw / stepDegrees) * stepDegrees);
        return new Rotations(p, r, y);
    }

    /**
     * Normalizes degrees to [0, 360] and cleans floating point inaccuracies.
     */
    public static float normalizeDegrees(float degrees) {
        return normalizeDegrees(degrees, 0.05f);
    }

    /**
     * Normalizes degrees to [0, 360] and snaps to the nearest integer if within snapTolerance.
     */
    public static float normalizeDegrees(float degrees, float snapTolerance) {
        degrees = degrees % 360.0f;
        if (degrees < 0.0f) {
            degrees += 360.0f;
        }
        float nearestInt = (float) Math.round(degrees);
        if (Math.abs(degrees - nearestInt) <= snapTolerance) {
            degrees = nearestInt;
        } else {
            degrees = (float) (Math.round(degrees * 10000.0) / 10000.0);
        }
        if (degrees >= 360.0f) {
            degrees -= 360.0f;
        }
        return degrees;
    }
}
