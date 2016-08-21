package io.piotrjastrzebski.tilemovementtest;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;

public class TileGame extends ApplicationAdapter implements InputProcessor {
	private static final String TAG = TileGame.class.getSimpleName();
	private final static int MAP_WIDTH = 8;
	private final static int MAP_HEIGHT = 10;
	private static int[][] MAP = { // [y][x]
		{1, 1, 1, 1, 1, 1, 1, 1,},
		{1, 0, 1, 0, 0, 0, 0, 1,},
		{1, 1, 0, 0, 0, 0, 0, 1,},
		{1, 0, 0, 0, 0, 0, 0, 1,},
		{1, 0, 0, 0, 0, 0, 0, 1,},
		{1, 0, 0, 0, 0, 0, 0, 1,},
		{1, 0, 0, 0, 0, 0, 0, 1,},
		{1, 1, 0, 0, 0, 0, 0, 1,},
		{1, 1, 1, 0, 0, 0, 0, 1,},
		{1, 1, 1, 1, 1, 1, 1, 1,},
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
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		float dt = Gdx.graphics.getDeltaTime();

		renderer.setProjectionMatrix(gameCamera.combined);
		batch.setProjectionMatrix(gameCamera.combined);

		renderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = 0; x < MAP_WIDTH; x++) {
			for (int y = 0; y < MAP_HEIGHT; y++) {
				renderer.setColor(Color.GRAY);
				renderer.rect(x, y, 1, 1);
				int type = MAP[y][x];
				switch (type) {
				case 0: {
					renderer.setColor(Color.GREEN);
				} break;
				case 1: {
					renderer.setColor(Color.ORANGE);
				} break;
				}
				renderer.rect(x + .1f, y + .1f, .8f, .8f);
			}
		}

//		renderer.setColor(Color.BLUE);
//		renderer.circle(mp.x, mp.y, .5f, 16);
//		renderer.setColor(Color.GREEN);
//		renderer.circle(tp.x, tp.y, .35f, 16);
//
//		tmp1.set(tp.x, tp.y);
//		tmp2.set(mp.x, mp.y);
//		renderer.setColor(Color.BLACK);
//		renderer.rectLine(tmp1, tmp2, .05f);

//		progress+= dt;
//		if (progress > 2) {
//			progress -= 2;
//		}
//		float a = progress <= 1?progress:2-progress;
//		tmp3.set(tmp1).lerp(tmp2, a);
//		renderer.setColor(Color.BLACK);
//		renderer.circle(tmp3.x, tmp3.y, .25f, 16);

//		target.set(mp.x, mp.y);
//		float dst2 = pos.dst2(target);
//		tmp1.set(target).sub(pos).nor();
//		final float minDst2 = 2;
//		final float speed = 10;
//		if (dst2 < minDst2) {
//			float a = dst2/minDst2;
//			tmp1.scl(speed * Interpolation.exp5Out.apply(a));
//		} else {
//			tmp1.scl(speed);
//		}
//		pos.mulAdd(tmp1, dt);
//
//		renderer.setColor(Color.WHITE);
//		renderer.circle(pos.x, pos.y, .25f, 16);


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
