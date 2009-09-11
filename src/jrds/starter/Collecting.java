package jrds.starter;


public class Collecting extends Starter {
	@Override
	public Object getKey() {
		return Collecting.class;
	}

	/* (non-Javadoc)
	 * @see jrds.starter.Starter#isStarted()
	 */
	@Override
	public boolean isStarted() {
		boolean upStarterd = true;
		StartersSet up = getLevel().getUp();
		if(up != null)
			upStarterd = up.isStarted(Collecting.class);
		return upStarterd && super.isStarted()  && ! Thread.currentThread().isInterrupted();
	}

	static public Object makeKey(StarterNode node) {
		return Collecting.class;
	}
}
