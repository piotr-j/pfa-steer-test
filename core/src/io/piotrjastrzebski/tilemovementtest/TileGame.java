package io.piotrjastrzebski.tilemovementtest;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;

public class TileGame extends ApplicationAdapter implements InputProcessor {
	private static final String TAG = TileGame.class.getSimpleName();
	private final static int MAP_WIDTH = 25;
	private final static int MAP_HEIGHT = 19;
	private final static int __ = 0;
	private final static int WL = 1;
	private static int[][] MAP = { // [y][x]
		{WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
		{WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, },
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

	}

	float progress;
	Vector2 tmp1 = new Vector2();
	Vector2 tmp2 = new Vector2();
	Vector2 tmp3 = new Vector2();

	Vector2 target = new Vector2();
	Vector2 pos = new Vector2();
	Vector2 accel = new Vector2();
	Vector2 vel = new Vector2();

	boolean forward = true;
	@Override
	public void render () {
		Gdx.gl.glClearColor(WL, __, __, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		float dt = Gdx.graphics.getDeltaTime();

		renderer.setProjectionMatrix(gameCamera.combined);
		batch.setProjectionMatrix(gameCamera.combined);

		renderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = 0; x < MAP_WIDTH; x++) {
			for (int y = 0; y < MAP_HEIGHT; y++) {
				renderer.setColor(Color.GRAY);
				renderer.rect(x, y, WL, 1);
				int type = MAP[y][x];
				switch (type) {
				case __: {
					renderer.setColor(Color.WHITE);
				} break;
				case WL: {
					renderer.setColor(Color.BLACK);
				} break;
				}
				renderer.rect(x + .075f, y + .075f, .85f, .85f);
			}
		}


		renderer.end();

		stage.act(dt);
		stage.draw();
	}
	
	@Override
	public void dispose () {
		VisUI.dispose();
		renderer.dispose();
		batch.dispose();
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
	}

	Vector3 tp = new Vector3();
	Vector3 mp = new Vector3();
	@Override public boolean keyDown (int keycode) {
		switch (keycode) {
		case Input.Keys.T: {
			int x = MathUtils.clamp((int)mp.x, 0, MAP_WIDTH);
			int y = MathUtils.clamp((int)mp.y, 0, MAP_HEIGHT);
			int type = MAP[y][x];
			if (type == 0) {
				MAP[y][x] = 1;
			} else if (type == 1) {
				MAP[y][x] = 0;
			}
		} break;
		case Input.Keys.P: {
			System.out.println("\tprivate static int[][] MAP = { // [y][x]");
			for (int x = MAP_WIDTH -1; x >= 0; x--) {
				System.out.print("\t\t{");
				for (int y = 0; y < MAP_HEIGHT; y++) {
					int type = MAP[y][x];
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

	@Override public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		gameCamera.unproject(tp.set(screenX, screenY, 0));
		return false;
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
}
