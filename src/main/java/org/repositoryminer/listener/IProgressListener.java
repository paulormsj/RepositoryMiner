package org.repositoryminer.listener;

/**
 * <h1>IProgressListener is a mining progress monitoring interface</h1>
 * <p>
 * Since RepositoryMiner is <b>NOT</b> intended to be executed directly, having
 * the actual purpose of serving as an API for other applications (<i>e.g.</i>,
 * Visminer Dashboard), it is important to provide a general contract for
 * listeners.
 * <p>
 * Front-end applications can implement the listener to meet specific monitoring
 * needs, such as animating progress bars and/or activate routines in response
 * to progress changes.
 * <p>
 * Listeners can be injected into
 * {@link org.repositoryminer.mining.RepositoryMiner#setProgressListener(IProgressListener)}
 * in order to be activated.
 * <p>
 * 
 * @since 2016-07-27
 */
public interface IProgressListener {

	/**
	 * Notifies the initiation of mining
	 * 
	 * @param name
	 *            the name of the project being mined
	 */
	public void initMining(String name);

	/**
	 * Notifies the progress of commits processing
	 * 
	 * @param commitIndex
	 *            the current index of the commit being processed
	 * @param numberOfCommits
	 *            total amount of commits to be processed
	 */
	public void commitsProgressChange(int commitIndex, int numberOfCommits);

	/**
	 * Notifies that the processing of timeframes has started
	 */
	public void initTimeFramesProgress();

	/**
	 * Notifies the progress of timeframes processing
	 * 
	 * @param timeFrameIndex
	 *            the current index of the timeframe being processed
	 * @param numberOfTimeFrames
	 *            total number of time frames to be processed
	 */
	public void timeFramesProgressChange(int timeFrameIndex, int numberOfTimeFrames);

	/**
	 * Notified that the processing of source analysis has started
	 */
	public void initSourceAnalysisProgress();

	/**
	 * Notifies the progress of tags processing
	 * 
	 * @param tagIndex
	 *            the current index of the tag being processed
	 * @param numberOfTags
	 *            total number of tags to be processed
	 */
	public void tagsProgressChange(int tagIndex, int numberOfTags);

	/**
	 * Notifies that the progress of a post mining task has started
	 * 
	 * @param postMiningTaskName
	 *            the name of the task being started
	 */
	public void initPostMiningTaskProgress(String postMiningTaskName);

	/**
	 * Notifies the progress of post mining tasks processing
	 * 
	 * @param taskStepIndex
	 *            the current index of the task's step being processed
	 * @param numberOfTaskSteps
	 *            total number of task's steps to be processed
	 */
	public void postMiningTaskProgressChange(int taskStepIndex, int numberOfTaskSteps);

	/**
	 * Notifies that the mining of the project has ended
	 */
	public void endOfMining();
}