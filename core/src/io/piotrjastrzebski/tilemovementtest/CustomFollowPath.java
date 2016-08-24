package io.piotrjastrzebski.tilemovementtest;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.utils.Path;
import com.badlogic.gdx.math.Vector;

/**
 * Created by PiotrJ on 24/08/16.
 */
public class CustomFollowPath <T extends Vector<T>, P extends Path.PathParam> extends FollowPath<T, P> {
	protected T itp;
	public CustomFollowPath (Steerable<T> owner, Path<T, P> path) {
		super(owner, path);
	}

	public CustomFollowPath (Steerable<T> owner, Path<T, P> path, float pathOffset) {
		super(owner, path, pathOffset);
	}

	public CustomFollowPath (Steerable<T> owner, Path<T, P> path, float pathOffset, float predictionTime) {
		super(owner, path, pathOffset, predictionTime);
		itp = getInternalTargetPosition();
	}

	@Override protected SteeringAcceleration<T> calculateRealSteering (SteeringAcceleration<T> steering) {
		// Predictive or non-predictive behavior?
		T location = (predictionTime == 0) ?
			// Use the current position of the owner
			owner.getPosition()
			:
			// Calculate the predicted future position of the owner. We're reusing steering.linear here.
			steering.linear.set(owner.getPosition()).mulAdd(owner.getLinearVelocity(), predictionTime);

		// Find the distance from the start of the path
		float distance = path.calculateDistance(location, pathParam);

		// Offset it
		float targetDistance = distance + pathOffset;

		// Calculate the target position
		path.calculateTargetPosition(itp, pathParam, targetDistance);

		if (arriveEnabled && path.isOpen()) {
			if (pathOffset >= 0) {
				// Use Arrive to approach the last point of the path
				if (targetDistance > path.getLength() - decelerationRadius) return arrive(steering, itp);
			} else {
				// Use Arrive to approach the first point of the path
				if (targetDistance < decelerationRadius) return arrive(steering, itp);
			}
		}

		// Seek the target position
		steering.linear.set(itp).sub(owner.getPosition()).nor()
			.scl(getActualLimiter().getMaxLinearAcceleration());

		// No angular acceleration
		steering.angular = 0;

		// Output steering acceleration
		return steering;
	}
}
