package io.piotrjastrzebski.tilemovementtest;

import com.badlogic.gdx.*;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.*;
import com.badlogic.gdx.ai.steer.proximities.RadiusProximity;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;

import java.util.Iterator;

public class TileGame extends ApplicationAdapter implements InputProcessor {
	private static final String TAG = TileGame.class.getSimpleName();

	public final static float SCALE = 32f;
	public final static float INV_SCALE = 1.f/SCALE;
	public final static float VP_WIDTH = 800 * INV_SCALE;
	public final static float VP_HEIGHT = 600 * INV_SCALE;

	protected OrthographicCamera gameCamera;
	protected OrthographicCamera guiCamera;
	protected ExtendViewport gameViewport;
	protected ScreenViewport guiViewport;

	protected SpriteBatch batch;
	protected ShapeRenderer renderer;

	protected Stage stage;
	protected Table root;

	protected Graph graph;
	protected IndexedAStarPathFinder<Graph.Node> pathFinder;
	protected Graph.Path path = new Graph.Path();

	@Override
	public void create () {
		gameCamera = new OrthographicCamera();
		gameViewport = new ExtendViewport(VP_WIDTH, VP_HEIGHT, gameCamera);
		guiCamera = new OrthographicCamera();
		guiViewport = new ScreenViewport(guiCamera);

		batch = new SpriteBatch();
		renderer = new ShapeRenderer();
		VisUI.load();

		stage = new Stage(guiViewport, batch);
		root = new Table();
		root.setFillParent(true);
		stage.addActor(root);
		Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));


		graph = new Graph();
		pathFinder = new IndexedAStarPathFinder<Graph.Node>(graph);
		findPath(gx(pfStart.x), gy(pfStart.y), gx(pfDest.x), gy(pfDest.y), path);
	}

	private void findPath (int sx, int sy, int dx, int dy, Graph.Path path) {
		Graph.Node from = graph.get(sx, sy);
		Graph.Node to = graph.get(dx, dy);
		path.clear();
		pathFinder.searchNodePath(from, to, graph.heuristic, path);
	}

	Array<Dude> dudes = new Array<Dude>();

	private Vector2 pfStart = new Vector2(5f, 5f);
	private Vector2 pfDest = new Vector2(10f, 10f);
	private Vector2 tmo = new Vector2();

	boolean drawConnections = false;
	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		float dt = Gdx.graphics.getDeltaTime();
		GdxAI.getTimepiece().update(dt);
		renderer.setProjectionMatrix(gameCamera.combined);
		batch.setProjectionMatrix(gameCamera.combined);

		renderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = 0; x < Graph.MAP_WIDTH; x++) {
			for (int y = 0; y < Graph.MAP_HEIGHT; y++) {
				renderer.setColor(.5f, .55f, .5f, 1);
				renderer.rect(x - .5f, y - .5f, Graph.WL, 1);
				int type = graph.typeOf(x, y);
				switch (type) {
				case Graph.__: {
					renderer.setColor(Color.WHITE);
				} break;
				case Graph.WL: {
					renderer.setColor(Color.BLACK);
				} break;
				case Graph.DR: {
					renderer.setColor(Color.GRAY);
				} break;
				}
				renderer.rect(x + .025f - .5f, y + .025f - .5f, .95f, .95f);
			}
		}

		if (selected != null) {
			renderer.setColor(Color.FOREST);
			renderer.circle(selected.pos.x, selected.pos.y, .35f, 16);
		}
		for (Dude dude : dudes) {
			dude.update(dt);

			renderer.setColor(Color.GREEN);
			renderer.circle(dude.pos.x, dude.pos.y, .25f, 16);
			renderer.setColor(Color.BLACK);
			angleToVector(tmo, dude.getOrientation());
			Vector2 pos = dude.getPosition();
			tmo.scl(.25f);
			renderer.rectLine(pos.x, pos.y, pos.x + tmo.x, pos.y + tmo.y, .05f);
			renderer.setColor(Color.GREEN);
			Graph.Path path = dude.path;
			if (path.getCount() >= 2) {
				Graph.Node from = path.get(0);
				for (int i = 1, size = path.getCount(); i < size; i++) {
					Graph.Node to = path.get(i);
					renderer.rectLine(from.x, from.y, to.x, to.y, .05f);
					from = to;
				}
			}
		}

		renderer.setColor(Color.RED);
		renderer.circle(pfStart.x, pfStart.y, .1f, 8);
		renderer.setColor(Color.BLUE);
		renderer.circle(pfDest.x, pfDest.y, .1f, 9);

		renderer.end();

		renderer.begin(ShapeRenderer.ShapeType.Line);

		if (drawConnections) {
			for (int x = 0; x < Graph.MAP_WIDTH; x++) {
				for (int y = 0; y < Graph.MAP_HEIGHT; y++) {
					Graph.Node from = graph.get(x, y);
					if (from.type == Graph.__) {
						renderer.setColor(.33f, .33f, .33f, .33f);
					} else {
						renderer.setColor(.77f, .77f, .77f, .33f);
					}
					Array<Connection<Graph.Node>> connections = graph.getConnections(from);
					for (Connection<Graph.Node> connection : connections) {
						Graph.Node to = connection.getToNode();
						renderer.line(from.x, from.y, to.x, to.y);
					}
				}
			}
		}

		if (path.getCount() >= 2) {
			float a = 0;
			float step = 1f/(path.getCount() -1);
			Graph.Node from = path.get(0);
			for (int i = 1, size = path.getCount(); i < size; i++) {
				Graph.Node to = path.get(i);
				renderer.setColor(1 - a, 0, a, 1);
				renderer.line(from.x, from.y, to.x, to.y);
				a += step;
				from = to;
			}
		}

		for (Dude dude : dudes) {
			if (dude.steering != null) {
				drawSteering(renderer, dude.steering);
			}
		}

		renderer.end();

		stage.act(dt);
		stage.draw();
	}

	private void drawSteering (ShapeRenderer renderer, SteeringBehavior steering) {
		Steerable owner = steering.getOwner();
		Vector2 op = (Vector2)owner.getPosition();
		if (steering instanceof CompositeSteering) {
			CompositeSteering cs = (CompositeSteering)steering;
			Array<SteeringBehavior> all = cs.getAll();
			for (SteeringBehavior sb : all) {
				drawSteering(renderer, sb);
			}
		} else if (steering instanceof CollisionAvoidance) {
			CollisionAvoidance ca = (CollisionAvoidance)steering;
			// ffs private things :/
			try {
				Field field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstNeighbor");
				field.setAccessible(true);
				Steerable<Vector2> firstNeighbor = (Steerable<Vector2>)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstMinSeparation");
				field.setAccessible(true);
				float firstMinSeparation = (Float)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstDistance");
				field.setAccessible(true);
				float firstDistance = (Float)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstRelativePosition");
				field.setAccessible(true);
				Vector2 firstRelativePosition = (Vector2)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstRelativeVelocity");
				field.setAccessible(true);
				Vector2 firstRelativeVelocity = (Vector2)field.get(ca);
				renderer.setColor(Color.RED);
				if (firstNeighbor != null) {
					Vector2 fp = firstNeighbor.getPosition();
					renderer.circle(fp.x, fp.y, .3f, 16);
					renderer.circle(fp.x, fp.y, .35f, 16);
					renderer.circle(fp.x, fp.y, .36f, 16);
				}

			} catch (ReflectionException e) {
				e.printStackTrace();
			}
			RadiusProximity proximity = (RadiusProximity)ca.getProximity();
			float radius = proximity.getRadius();
			renderer.setColor(Color.ORANGE);

			renderer.circle(op.x, op.y, radius, 32);
		} else if (steering instanceof CustomFollowPath) {
			CustomFollowPath fp = (CustomFollowPath)steering;
			Vector2 itp = (Vector2)fp.getInternalTargetPosition();
			renderer.setColor(Color.RED);
			renderer.circle(itp.x, itp.y, .2f, 16);
		} else if (steering instanceof Arrive) {

		}
	}

	@Override
	public void dispose () {
		VisUI.dispose();
		renderer.dispose();
		batch.dispose();
	}

	Dude selected;
	private static class Dude implements Steerable<Vector2> {
		Vector2 pos = new Vector2();
		float orientation;
		Vector2 vel = new Vector2();
		float zeroSpeed = 0.01f;
		float maxLinearSpeed = 2f;
		float maxLinearAccel = 20;
		float angVel = 0;
		float maxAngSpeed = 45 * MathUtils.degreesToRadians;
		float maxAngAccel = 15 * MathUtils.degreesToRadians;
		float radius = .3f;
		boolean tagged;

		Graph.Path path;
		SteeringBehavior<Vector2> steering;
		SteeringAcceleration<Vector2> acceleration;

		public Dude (float x, float y) {
			pos.set(x, y);
			orientation = MathUtils.random(-MathUtils.PI2, MathUtils.PI2);
			path = new Graph.Path();
			acceleration = new SteeringAcceleration<Vector2>(new Vector2(), 0);
		}

		public void update (float dt) {
			if (steering != null) {
				steering.calculateSteering(acceleration);
				pos.mulAdd(vel, dt);
				vel.mulAdd(acceleration.linear, dt).limit(maxLinearSpeed);

				// If we haven't got any velocity, then we can do nothing.
				if (!vel.isZero(getZeroLinearSpeedThreshold())) {
					float newOrientation = vectorToAngle(vel);
					angVel = (newOrientation - orientation) * dt; // this is superfluous if independentFacing is always true
					orientation = newOrientation;
				}
			}
		}

		@Override public Vector2 getLinearVelocity () {
			return vel;
		}

		@Override public float getAngularVelocity () {
			return angVel;
		}

		@Override public float getBoundingRadius () {
			return radius;
		}

		@Override public boolean isTagged () {
			return tagged;
		}

		@Override public void setTagged (boolean tagged) {
			this.tagged = tagged;
		}

		@Override public float getZeroLinearSpeedThreshold () {
			return zeroSpeed;
		}

		@Override public void setZeroLinearSpeedThreshold (float value) {
			zeroSpeed = value;
		}

		@Override public float getMaxLinearSpeed () {
			return maxLinearSpeed;
		}

		@Override public void setMaxLinearSpeed (float maxLinearSpeed) {
			this.maxLinearSpeed = maxLinearSpeed;
		}

		@Override public float getMaxLinearAcceleration () {
			return maxLinearAccel;
		}

		@Override public void setMaxLinearAcceleration (float maxLinearAcceleration) {
			maxLinearAccel = maxLinearAcceleration;
		}

		@Override public float getMaxAngularSpeed () {
			return maxAngSpeed;
		}

		@Override public void setMaxAngularSpeed (float maxAngularSpeed) {
			maxAngSpeed = maxAngularSpeed;
		}

		@Override public float getMaxAngularAcceleration () {
			return maxAngAccel;
		}

		@Override public void setMaxAngularAcceleration (float maxAngularAcceleration) {
			maxAngAccel = maxAngularAcceleration;
		}

		@Override public Vector2 getPosition () {
			return pos;
		}

		@Override public float getOrientation () {
			return orientation;
		}

		@Override public void setOrientation (float orientation) {
			this.orientation = orientation;
		}

		@Override public float vectorToAngle (Vector2 vector) {
			return TileGame.vectorToAngle(vector);
		}

		@Override public Vector2 angleToVector (Vector2 outVector, float angle) {
			return TileGame.angleToVector(outVector, angle);
		}

		@Override public Location<Vector2> newLocation () {
			return new DudeLocation();
		}
	}

	public static class DudeLocation implements Location<Vector2> {
		Vector2 pos = new Vector2();
		float orientation;

		@Override public Vector2 getPosition () {
			return pos;
		}

		@Override public float getOrientation () {
			return orientation;
		}

		@Override public void setOrientation (float orientation) {
			this.orientation = orientation;
		}

		@Override public float vectorToAngle (Vector2 vector) {
			return TileGame.vectorToAngle(vector);
		}

		@Override public Vector2 angleToVector (Vector2 outVector, float angle) {
			return TileGame.angleToVector(outVector, angle);
		}

		@Override public Location<Vector2> newLocation () {
			return new DudeLocation();
		}
	}

	public static float vectorToAngle (Vector2 vector) {
		// this is different than stuff in vector2, why is that? probably we want different axis
		return (float)Math.atan2(-vector.x, vector.y);
	}

	public static Vector2 angleToVector (Vector2 outVector, float angleRad) {
		outVector.x = -(float)Math.sin(angleRad);
		outVector.y = (float)Math.cos(angleRad);
		// same as
//		return outVector.set(0, 1).rotate(angleRad);
		return outVector;
	}

	public void enableBlending () {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	public void disableBlending () {
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	@Override public void resize (int width, int height) {
		gameViewport.update(width, height, true);
		guiViewport.update(width, height, true);

		gameViewport.getCamera().translate(-.5f, -.5f, 0);
		gameViewport.getCamera().update();
	}

	static int gx(float value) {
		return MathUtils.clamp(MathUtils.round(value), 0, Graph.MAP_WIDTH - 1);
	}

	static int gy(float value) {
		return MathUtils.clamp(MathUtils.round(value), 0, Graph.MAP_HEIGHT - 1);
	}

	static float tx(float value) {
		return gx(value) - .5f;
	}

	static float ty(float value) {
		return gy(value) - .5f;
	}

	Vector3 tp = new Vector3();
	Vector3 mp = new Vector3();
	Rectangle tmpRect = new Rectangle(0, 0, 1, 1);
	@Override public boolean keyDown (int keycode) {
		switch (keycode) {
		case Input.Keys.T: {
			int x = gx(mp.x);
			int y = gy(mp.y);
			int type = graph.typeOf(x, y);
			if (type == 0) {
				graph.setTypeOf(x, y, 1);
			} else if (type == 1) {
				graph.setTypeOf(x, y, 0);
			}
			findPath(gx(pfStart.x), gy(pfStart.y), gx(pfDest.x), gy(pfDest.y), path);
		} break;
		case Input.Keys.D: {
			Dude at = null;
			for (Dude dude : dudes) {
				tmpRect.setPosition(tx(dude.pos.x), ty(dude.pos.y));
				if (tmpRect.contains(mp.x, mp.y)) {
					at = dude;
					break;
				}
			}

			int x = gx(mp.x);
			int y = gy(mp.y);
			if (at == null && graph.typeOf(x, y) == Graph.__) {
				Dude dude = new Dude(x, y);
				DudeLocation location = new DudeLocation();
				location.getPosition().set(dude.getPosition());
				location.getPosition().add(0, 1);
				Arrive<Vector2> arrive = new Arrive<Vector2>(dude, location);
				arrive.setTimeToTarget(.1f);
				arrive.setArrivalTolerance(.001f);
				arrive.setDecelerationRadius(.5f);
				CollisionAvoidance<Vector2> avoidance = new CollisionAvoidance<Vector2>(dude,
					new RadiusProximity<Vector2>(dude, dudes, .75f));
//				MyPrioritySteering<Vector2> steering = new MyPrioritySteering<Vector2>(dude, .001f);
//				steering.add(avoidance);
//				steering.add(arrive);
				MyBlendedSteering<Vector2> steering = new MyBlendedSteering<Vector2>(dude);
				steering.add(avoidance, 5);
				steering.add(arrive, 1);
				dude.steering = steering;
				dudes.add(dude);
			}
		} break;
		case Input.Keys.P: {
			graph.print();
		} break;
		case Input.Keys.W: {
			if (selected != null) {
				path(selected, mp.x, mp.y);
			}
		} break;
		}
		return false;
	}

	@Override public boolean keyUp (int keycode) {
		return false;
	}

	@Override public boolean keyTyped (char character) {
		return false;
	}

	Circle circle = new Circle();
	@Override public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		gameCamera.unproject(tp.set(screenX, screenY, 0));
		if (button == Input.Buttons.LEFT) {
			Dude selected = null;
			for (Dude dude : dudes) {
				circle.set(dude.pos.x, dude.pos.y, .5f);
				if (circle.contains(tp.x, tp.y)) {
					selected = dude;
					break;
				}
			}
			if (selected == null && this.selected == null) {
				pfStart.set(gx(tp.x), gy(tp.y));
			}
			this.selected = selected;
		} else if (button == Input.Buttons.RIGHT) {
			if (selected != null) {
				path(selected, tp.x, tp.y);
			} else {
				pfDest.set(gx(tp.x), gy(tp.y));
			}
		}
		findPath(gx(pfStart.x), gy(pfStart.y), gx(pfDest.x), gy(pfDest.y), path);

		return false;
	}

	private void path (Dude dude, float x, float y) {
		dude.path.clear();
		findPath(gx(dude.pos.x), gy(dude.pos.y), gx(x), gy(y), dude.path);
		if (dude.path.getCount() < 2) {
			return;
		}
		// hpw dp we dp doors? custom path? could return point before door, until it is open i guess
		CustomLinePath<Vector2> linePath = new CustomLinePath<Vector2>(dude.path.toV2Path(), true);
		CustomFollowPath<Vector2, CustomLinePath.LinePathParam> followPath = new CustomFollowPath<Vector2, CustomLinePath.LinePathParam>(
			dude, linePath, .25f, .25f);
		followPath
			.setDecelerationRadius(.65f)
			.setArrivalTolerance(.01f)
			.setArriveEnabled(true)
			.setTimeToTarget(.1f);
		CollisionAvoidance<Vector2> avoidance = new CollisionAvoidance<Vector2>(dude,
			new RadiusProximity<Vector2>(dude, dudes, .8f));
//					MyPrioritySteering<Vector2> steering = new MyPrioritySteering<Vector2>(selected, .0001f);
		LookWhereYouAreGoing<Vector2> lookWhereYouAreGoing = new LookWhereYouAreGoing<Vector2>(dude);
		lookWhereYouAreGoing.setAlignTolerance(0.1f);
		MyBlendedSteering<Vector2> steering = new MyBlendedSteering<Vector2>(dude);
		steering.add(avoidance, 4);
		steering.add(followPath, 1);
		steering.add(lookWhereYouAreGoing, 1);
		dude.steering = steering;
	}

	@Override public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		gameCamera.unproject(tp.set(screenX, screenY, 0));
		return false;
	}

	@Override public boolean touchDragged (int screenX, int screenY, int pointer) {
		gameCamera.unproject(tp.set(screenX, screenY, 0));
		return false;
	}

	@Override public boolean mouseMoved (int screenX, int screenY) {
		gameCamera.unproject(mp.set(screenX, screenY, 0));
		return false;
	}

	@Override public boolean scrolled (int amount) {
		return false;
	}

	public static class Graph implements IndexedGraph<Graph.Node> {
		public final static int MAP_WIDTH = 25;
		public final static int MAP_HEIGHT = 19;
		public final static int __ = 0;
		public final static int WL = 1;
		public final static int DR = 2;
		private static int[][] MAP = { // [y][x]
			{WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, },
			{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, WL, __, WL, __, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, WL, __, __, WL, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, WL, __, WL, __, WL, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, __, __, WL, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, WL, WL, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, __, WL, __, __, __, __, __, WL, },
			{WL, __, __, WL, __, __, WL, __, __, __, __, __, __, __, __, __, WL, WL, WL, __, __, __, __, __, WL, },
			{WL, WL, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, WL, __, __, __, __, __, __, __, __, __, __, __, __, WL, WL, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, WL, DR, WL, WL, __, __, WL, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, WL, __, __, WL, __, WL, __, WL, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, WL, __, __, WL, __, __, WL, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, WL, WL, DR, WL, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
			{WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, },
		};
		static {
			// flip y so it looks ok
			for (int y = 0; y < MAP_HEIGHT/2; y++) {
				int[] ints = MAP[y];
				MAP[y] = MAP[MAP_HEIGHT - y - 1];
				MAP[MAP_HEIGHT - y - 1] = ints;
			}
		}

		Node[][] nodes = new Node[MAP_HEIGHT][MAP_WIDTH];
		Heuristic<Node> heuristic = new Heuristic<Node>() {
			@Override public float estimate (Node node, Node endNode) {
				return Vector2.dst(node.x, node.y, endNode.x, endNode.y);
			}
		};
		public Graph () {
			for (int y = 0; y < Graph.MAP_HEIGHT; y++) {
				Node[] inner = nodes[y] = new Node[MAP_WIDTH];
				for (int x = 0; x < Graph.MAP_WIDTH; x++) {
					inner[x] = new Node(x, y, typeOf(x, y));
				}
			}
		}

		@Override public int getIndex (Node node) {
			return node.x + node.y * MAP_WIDTH;
		}

		@Override public int getNodeCount () {
			return MAP_WIDTH * MAP_HEIGHT;
		}

		public Node get(int x, int y) {
			return nodes[y][x];
		}

		@Override public Array<Connection<Node>> getConnections (Node fromNode) {
			if (fromNode.connections == null) {
				fromNode.connections = new Array<Connection<Node>>();
				for (int oy = -1; oy <= 1; oy++) {
					for (int ox = -1; ox <= 1; ox++) {
						if (ox == 0 && oy == 0) continue;
						int x = fromNode.x + ox;
						int y = fromNode.y + oy;
						if (x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT) {
							int type = typeOf(x, y);
							if (type == fromNode.type || (type == DR && fromNode.type == __) || (type == __ && fromNode.type == DR)) {
								if (Math.abs(ox) == Math.abs(oy)) {
									if (typeOf(fromNode.x + ox, fromNode.y) == type && typeOf(fromNode.x, fromNode.y + oy) == type) {
										fromNode.connections.add(new NodeConnection(fromNode, get(x, y)));
									}
								} else {
									fromNode.connections.add(new NodeConnection(fromNode, get(x, y)));
								}
							}
						}
					}
				}
			}
			return fromNode.connections;
		}

		public int typeOf (int x, int y) {
			return MAP[y][x];
		}

		public void setTypeOf (int x, int y, int type) {
			MAP[y][x] = type;
			Node node = nodes[y][x];
			node.type = type;
			// rebuild surrounding connections when needed
			for (int oy = -1; oy <= 1; oy++) {
				for (int ox = -1; ox <= 1; ox++) {
					if (ox == 0 && oy == 0) continue;
					int cx = x + ox;
					int cy = y + oy;
					if (cx >= 0 && cx < MAP_WIDTH && cy >= 0 && cy < MAP_HEIGHT) {
						get(cx, cy).connections = null;
					}
				}
			}
		}

		public void print () {
			System.out.println("\tprivate static int[][] MAP = { // [y][x]");
			for (int x = MAP_WIDTH -1; x >= 0; x--) {
				System.out.print("\t\t{");
				for (int y = 0; y < MAP_HEIGHT; y++) {
					int type = typeOf(x, y);
					switch (type) {
					case WL: {
						System.out.print("WL, ");
					}
					break;
					case __: {
						System.out.print("__, ");
					}
					break;
					}
				}
				System.out.println("},");
			}
			System.out.println("\t};");
		}

		public static class Node {
			int x;
			int y;
			int type;
			Array<Connection<Node>> connections;

			public Node (int x, int y, int type) {
				this.x = x;
				this.y = y;
				this.type = type;
			}
		}

		public static class NodeConnection implements Connection<Node> {
			Node from;
			Node to;
			float cost;
			public NodeConnection (Node from, Node to) {
				this.from = from;
				this.to = to;
				cost = 1.41f;
				if (to.x == from.x || to.y == from.y) {
					cost = 1;
				}
			}

			@Override public float getCost () {
				return cost;
			}

			@Override public Node getFromNode () {
				return from;
			}

			@Override public Node getToNode () {
				return to;
			}
		}

		public static class Path implements GraphPath<Node> {
			Array<Node> nodes = new Array<Node>();
			@Override public int getCount () {
				return nodes.size;
			}

			@Override public Node get (int index) {
				return nodes.get(index);
			}

			@Override public void add (Node node) {
				nodes.add(node);
			}

			@Override public void clear () {
				nodes.clear();
			}

			@Override public void reverse () {
				nodes.reverse();
			}

			@Override public Iterator<Node> iterator () {
				return nodes.iterator();
			}

			Array<Vector2> toV2Path() {
				Array<Vector2> array = new Array<Vector2>();
				for (Node node : nodes) {
					array.add(new Vector2(node.x, node.y));
				}
				return array;
			}
		}
	}

	public interface CompositeSteering<T extends Vector<T>> {
		Array<SteeringBehavior<T>> getAll();
	}

	public static class MyBlendedSteering<T extends Vector<T>> extends BlendedSteering<T> implements CompositeSteering<T> {
		/**
		 * Creates a {@code BlendedSteering} for the specified {@code owner}, {@code maxLinearAcceleration} and
		 * {@code maxAngularAcceleration}.
		 *
		 * @param owner the owner of this behavior.
		 */
		public MyBlendedSteering (Steerable<T> owner) {
			super(owner);
		}

		private Array<SteeringBehavior<T>> all = new Array<SteeringBehavior<T>>();
		public Array<SteeringBehavior<T>> getAll() {
			all.clear();
			for (BehaviorAndWeight<T> bw : list) {
				all.add(bw.getBehavior());
			}
			return all;
		}
	}

	public static class MyPrioritySteering<T extends Vector<T>>  extends PrioritySteering<T> implements CompositeSteering<T> {
		public MyPrioritySteering (Steerable<T> owner) {
			super(owner);
		}

		public Array<SteeringBehavior<T>> getAll () {
			return behaviors;
		}
	}
}
