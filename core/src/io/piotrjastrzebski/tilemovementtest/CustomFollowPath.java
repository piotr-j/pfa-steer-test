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
	protected T internal;
	public CustomFollowPath (Steerable<T> owner, Path<T, P> path) {
		super(owner, path);
	}

	public CustomFollowPath (Steerable<T> owner, Path<T, P> path, float pathOffset) {
		super(owner, path, pathOffset);
	}

	public CustomFollowPath (Steerable<T> owner, Path<T, P> path, float pathOffset, float predictionTime) {
		super(owner, path, pathOffset, predictionTime);
		internal = getInternalTargetPosition();
	}

	@Override protected SteeringAcceleration<T> calculateRealSteering (SteeringAcceleration<T> steering) {
		return super.calculateRealSteering(steering);
	}
}
