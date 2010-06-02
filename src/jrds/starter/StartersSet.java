package jrds.starter;


public interface StartersSet {
	public Starter registerStarter(Starter s, StarterNode parent);
	public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, StarterNode node);
	public Starter find(Object key);
	public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, Object key);
	public boolean isStarted(Object key);
	public void setParent(StartersSet s);
	public StartersSet getParent();
	public StarterNode getLevel();
}