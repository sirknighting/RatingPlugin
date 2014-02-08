package com.gmail.sirknightingfail.rating;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RatingPlugin extends JavaPlugin implements Listener{
	private FileConfiguration scores = null;
	private File scoresFile = null;
	private File ranksFile = null;
	private FileConfiguration ranks = null;
	private int startingScore;
	private int changeRate;
	private int exponentDiff;
	private int floorScore;
	private int updateFrequency;
	private BukkitTask update_task;
	private HashMap<Player,Player> lastHit= new HashMap<Player,Player>();
	@Override
	public void onEnable(){

		this.saveDefaultConfig();
		this.getLogger().info("SirksRatingPlugin has been activated");
		this.getConfig();
		this.getScores();
		this.getRanks();
		if(!this.getScores().contains("changed")){
			this.getScores().createSection("changed");
		}
		if(!this.getScores().contains("players")){
			this.getScores().createSection("players");
		}
		changeRate=this.getConfig().getInt("standardDiffMatch");
		startingScore=this.getConfig().getInt("initialScore");
		exponentDiff=this.getConfig().getInt("exponentDiff");
		updateFrequency=this.getConfig().getInt("rankUpdateFrequency");
		/*the next area is the updater task. MAKE IT SO THE TIME IS THE SAME AS IN THE CONFIG*/
		update_task=this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable(){
			@Override
			public void run(){
				updateRanks();
			}
		}
		,0L,20*updateFrequency);

	}
	/**
	 * Scores.yml is set up as follows.
	 * changed:
	 *   -List of players whose ranks have changed.
	 *   -
	 *   -
	 * Playername:
	 *   score: (int)
	 *   changed: (boolean) 
	 *   tourneyScore: double, made by actual score minus expected score
	 *   kills: (int)
	 *   deaths: (int)
	 *   kdr: (double), if deaths is 0, this value is made 0.
	 * 
	 * Maybe Remove the ranks within here.*/
	/**ranks.yml has the following lists:
	 * score:
	 *   -
	 *   -
	 *   -
	 * kills:
	 *   -
	 *   -
	 *   -
	 * kdr:
	 *   -
	 *   -
	 *   -
	 *   */
	public boolean onCommand(CommandSender sender,Command cmd,String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("pvprank")){
			if(sender.hasPermission("pvprank.use")){
				if(args.length==0){
					return false;
				}
				if(args[0].equalsIgnoreCase("stats")){
					if(args.length>2){
						sender.sendMessage("¤cInvalid Arguments!");
						sender.sendMessage("¤aUsage: /pvprank stats [playername]");
						return true;
					}
					String examined="";
					if(args.length==2){examined=args[1];}
					else examined=sender.getName();
					if(this.getScores().contains("players."+examined+".score")){
						int kills=this.getScores().getInt("players."+examined+".kills");
						int deaths=this.getScores().getInt("players."+examined+".deaths");
						String kdr="";
						if(deaths!=0){
							kdr=""+round(kills/deaths,4);
						}
						else kdr="undefined";
						sender.sendMessage("¤e"+examined+"\'s Stats");
						sender.sendMessage("¤eScore: ¤2"+this.getScores().getInt("players."+examined+".score"));
						sender.sendMessage("¤eKills: ¤2"+kills);
						sender.sendMessage("¤eDeaths: ¤2"+deaths);
						sender.sendMessage("¤eKill/Death Ratio:¤2 "+kdr);
						sender.sendMessage("¤eScore Ranking:¤2 "+(this.getRanks().getList("score").indexOf(examined)+1));
						sender.sendMessage("¤eKills Ranking:¤2 "+(this.getRanks().getList("kills").indexOf(examined)+1));
						sender.sendMessage("¤eKill/Death Ranking:¤2 "+(this.getRanks().getList("kdr").indexOf(examined)+1));
						sender.sendMessage("¤eScore-Rankings and scores are as of the last time ranks were updated.");
						return true;
					}
					else{
						sender.sendMessage("¤cThat player is yet to die or kill in battle.");
						return true;
					}
				}
				if(args[0].equalsIgnoreCase("top")){
					if(args.length==1){
						List<String> scoreTop=this.getRanks().getStringList("score");

						int listLength=10;
						if(listLength>scoreTop.size()){listLength=scoreTop.size();}
						/**In Minecraft, three spaces has the same length as two digits.*/
						sender.sendMessage("¤eTop "+listLength+" Score-wise");
						for(int a=1;a<=listLength;a++){
							String playerName=scoreTop.get(a-1);
							sender.sendMessage("¤e"+formatToLength(a,(int)Math.log10(listLength))+". ¤a"+playerName);
						}
						/*Add a final message over here.*/
						return true;
					}
					if(args.length==2&&args[1]!=null){
						if(args[1].equalsIgnoreCase("score")||args[1].equalsIgnoreCase("kills")||args[1].equalsIgnoreCase("kdr")){
							for(int a=0;a<=args[2].length()-1;a++){
								if(DIGITS.indexOf(args[2].substring(a,a+1))==-1){
									sender.sendMessage("¤cThe Page Number Must Be An Integer!");
									return false;
								}
							}
							int pageNumber=Integer.parseInt(args[2]);
							String listName=args[1].toLowerCase();
							List<String> scoreTop=this.getRanks().getStringList(listName);
							if(pageNumber>(int)(scoreTop.size()/10+1)){
								sender.sendMessage("¤cInvalid Page Number");
								return false;
							}
							int pageEnd=10*pageNumber;
							if(pageEnd>scoreTop.size()){pageEnd=scoreTop.size();}
							/**In Minecraft, three spaces has the same length as two digits.*/
							sender.sendMessage("¤eTop "+(10*(pageNumber-1)+1)+"-"+pageEnd+" in "+args[1].toLowerCase()+":");
							for(int a=10*pageNumber-9;a<=pageEnd;a++){
								String playerName=scoreTop.get(a-1);
								sender.sendMessage("¤e"+formatToLength(a,(int)Math.log10(pageEnd))+" .¤a"+playerName);
							}
							if(listName.equals("kdr")){
								sender.sendMessage("¤ePlayers with no deaths are considered as having a kdr of 0.");
							}/*Add a final message over here.*/
							return true;
						}
						else{
							sender.sendMessage("¤cUsage: /pvprank top [score|kills|kdr] [pageNumber]");
							return true;
						}
					}
					if(args.length==3&&args[1]!=null&&args[2]!=null){
						if(args[1].equalsIgnoreCase("score")||args[1].equalsIgnoreCase("kills")||args[1].equalsIgnoreCase("kdr")){
							String listName=args[1].toLowerCase();
							List<String> scoreTop=this.getRanks().getStringList(listName);
							int listLength=10;
							if(listLength>scoreTop.size()){listLength=scoreTop.size();}
							/**In Minecraft, three spaces has the same length as two digits.*/
							sender.sendMessage("¤eTop "+listLength+" in "+args[1].toLowerCase()+":");
							for(int a=1;a<=listLength;a++){
								String playerName=scoreTop.get(a-1);
								sender.sendMessage("¤e"+formatToLength(a,(int)Math.log10(listLength))+" .¤a"+playerName);
							}
							/*Add a final message over here.*/
							return true;
						}
						else{
							sender.sendMessage("¤cUsage: /pvprank top [score|kills|kdr] [pageNumber]");
							return true;
						}
					}
					return false;
					/**The options for top are:
					 *  /pvprank top [kills|score|kdr] [number]
					 *  the number is the page number. it by default displays 10 lines.*/
				}
				if(args[0].equalsIgnoreCase("reload")){
					if(sender.hasPermission("pvprank.reload")){
						this.reloadConfig();
						this.reloadRanks();
						this.reloadScores();
						changeRate=this.getConfig().getInt("standardDiffMatch");
						startingScore=this.getConfig().getInt("initialScore");
						sender.sendMessage("¤2Configuration reloaded.");
						return true;
					}
					else{
						sender.sendMessage("¤cYou do not have permission to reload the pvprank configuration");
						return false;
					}
				}
				/**Add the other command sub-parts here, soon.**/
			}
		}
		return false;
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void deathListener(PlayerDeathEvent death){
		Player loser=death.getEntity();
		loser.sendMessage("You died");
		EntityDamageEvent event=loser.getLastDamageCause();//TRY USING THIS, detect the type of kill, maybe using hashmaps
		if(event instanceof EntityDamageByEntityEvent){
			
		}
        String loserName=loser.getName();
//		int b=a.length()-1;
//		for(;!a.substring(b,b+1).equals(" ");b--){
//		}
//		String winnerName=a.substring(b+1);
        
        if(event instanceof EntityDamageByEntityEvent){
        	//FIX THIS PART
        }
		String winnerName="";
		Player winner=this.getServer().getPlayerExact(winnerName);
		if(winner!=null){
			int multiplier=1;
			if(!this.getScores().contains("players."+winnerName+".score")){
				createPlayerSection(winnerName);
			}
			if(!this.getScores().contains("players."+loserName+".score")){
				createPlayerSection(loserName);
			}
			/**ItemStack[] loserStuff=death.getDrops().toArray(new ItemStack[death.getDrops().size()]);
			 * Add the part that equates the scores based on the amount of uses.
			 * This should end with a double called
			 * multiplier, which the difference between the two is multiplied by.
			 * */
			/*
			 * ORGANIZE THIS
			 * */
			int winnerScore=this.getScores().getInt("players."+winnerName+".score");
			int loserScore=this.getScores().getInt("players."+loserName+".score");
			double expectedWin=1/(1+Math.pow(10,(loserScore-winnerScore)*multiplier/exponentDiff));
			double oldTourneyOfWinner=this.getScores().getDouble("players."+winnerName+".tourneyScore");
			double oldTourneyOfLoser=this.getScores().getDouble("players."+loserName+".tourneyScore");
			/**The following is where the scores for the update period are stored.*/
			if(!(loserScore-((int)(changeRate*(expectedWin-1+oldTourneyOfLoser)+0.5))<floorScore)){
				/**Add the prevention from falling below the floor score.*/
				this.getScores().set("players."+winnerName+".tourneyScore", 1-expectedWin+oldTourneyOfWinner);
				this.getScores().set("players."+loserName+".tourneyScore", expectedWin-1+oldTourneyOfLoser);
			}
			/*Add the part where the tourney score is put in.*/ 
			int oldDeaths=this.getScores().getInt("players."+loserName+".deaths");
			int oldKills=this.getScores().getInt("players."+winnerName+".kills");
			oldKills++;
			oldDeaths++;
			this.getScores().set("players."+loserName+".deaths",oldDeaths);
			this.getScores().set("players."+winnerName+".kills", oldKills);
			this.getScores().set("players."+loserName+".kdr", this.getScores().getInt("players."+loserName+".kills")/oldDeaths);
			double winnerKdr=0;
			int winnerDeaths=this.getScores().getInt("players."+winnerName+".deaths");
			if(winnerDeaths!=0){
				winnerKdr=oldKills/winnerDeaths;
			}
			this.getScores().set("players."+winnerName+".kdr", winnerKdr);
			List<String> changedList=this.getScores().getStringList("changed");
			changedList.add(loserName);
			changedList.add(winnerName);
			int newWinnerIndex=this.getRanks().getStringList("kills").indexOf(winnerName);
			int listSize=this.getRanks().getStringList("score").size();
			List<String> newKillsRank=this.getRanks().getStringList("kills");
			newKillsRank.remove(newWinnerIndex);
			while(newWinnerIndex>0&&oldKills>this.getScores().getDouble("players."+this.getRanks().getStringList("kills").get(newWinnerIndex-1)+".kills")){
				newWinnerIndex--;
			}
			newKillsRank.add(newWinnerIndex,winnerName);
			/**Finish this section.*/
			this.getRanks().set("kills", newKillsRank);
			int newWinnerKdrIndex=this.getRanks().getStringList("kdr").indexOf(winnerName);
			List<String> newWinnerKdrRank=this.getRanks().getStringList("kdr");
			newWinnerKdrRank.remove(newWinnerKdrIndex);
			while(newWinnerKdrIndex>0&&winnerKdr>this.getScores().getDouble("players."+this.getRanks().getStringList("kdr").get(newWinnerKdrIndex-1)+".kdr")){
				newWinnerKdrIndex--;
			}
			while(newWinnerKdrIndex<listSize-1&&winnerKdr<this.getScores().getDouble("players."+this.getRanks().getStringList("kdr").get(newWinnerKdrIndex)+".kdr")){
				newWinnerKdrIndex++;
			}
			newWinnerKdrRank.add(newWinnerKdrIndex,winnerName);
			/**Finish this section.*/
			
			this.getRanks().set("kdr", newWinnerKdrRank);
			int newLoserKdrIndex=this.getRanks().getStringList("kdr").indexOf(loserName);
			List<String> newLoserKdrRanks=this.getRanks().getStringList("kdr");
			newLoserKdrRanks.remove(newLoserKdrIndex);
			double loserKdr=(this.getScores().getInt("players."+loserName+".kills")/oldDeaths);
			while(newLoserKdrIndex<listSize-1&&loserKdr<this.getScores().getDouble("players."+this.getRanks().getStringList("kdr").get(newLoserKdrIndex)+".kdr")){
				newLoserKdrIndex++;
			}
			while(newLoserKdrIndex>=0&&loserKdr>this.getScores().getDouble("players."+this.getRanks().getStringList("kdr").get(newLoserKdrIndex-1)+".kdr")){
				newLoserKdrIndex--;
			}
			newLoserKdrRanks.add(newLoserKdrIndex,loserName);
			/**Finish this section.*/
			this.getRanks().set("kdr", newLoserKdrRanks);
			this.getScores().set("changed",changedList);
			/*Auto-update kills and kdr ranks.*/
			this.saveScores();
		}
		/*finish the expected score modifications For the equipped items.*/
	}
	@Override
	public void onDisable(){
		this.getLogger().info("SirksRatingPlugin has been de-activated");
		this.saveScores();
		this.saveRanks();
		this.saveConfig();
		update_task.cancel();
	}
	public void reloadScores() {
		if (scoresFile == null) {
			scoresFile = new File(getDataFolder(), "scores.yml");
		}
		scores = YamlConfiguration.loadConfiguration(scoresFile);

		// Look for defaults in the jar
		InputStream defConfigStream = this.getResource("scores.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			scores.setDefaults(defConfig);
		}
	}
	public FileConfiguration getScores() {
		if (scores == null) {
			this.reloadScores();
		}
		return scores;
	}
	public void saveScores() {
		if (scores == null || scoresFile == null) {
			return;
		}
		try {
			getScores().save(scoresFile);
		} catch (IOException ex) {
			this.getLogger().log(Level.SEVERE, "Could not save config to " + scoresFile, ex);
		}
	}
	public void reloadRanks() {
		if (ranksFile == null) {
			ranksFile = new File(getDataFolder(), "ranks.yml");
		}
		ranks = YamlConfiguration.loadConfiguration(ranksFile);

		// Look for defaults in the jar
		InputStream defConfigStream = this.getResource("ranks.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			ranks.setDefaults(defConfig);
		}
	}
	public FileConfiguration getRanks() {
		if (ranks == null) {
			this.reloadRanks();
		}
		return ranks;
	}
	public void saveRanks() {
		if (ranks == null || ranksFile == null) {
			return;
		}
		try {
			getRanks().save(ranksFile);
		} catch (IOException ex) {
			this.getLogger().log(Level.SEVERE, "Could not save config to " + ranksFile, ex);
		}
	}
	public void createPlayerSection(String playername){
		if(this.getScores().contains("players."+playername)){
			return;
		}
		else{
			this.getScores().createSection("players."+playername);
			this.getScores().set("players."+playername+".score",startingScore);
			this.getScores().set("players."+playername+".tourneyScore",0.0);
			this.getScores().set("players."+playername+".kills", 0);
			this.getScores().set("players."+playername+".deaths", 0);
			this.getScores().set("players."+playername+".kdr", 0);
			List<String> changedList=this.getScores().getStringList("changed");
			changedList.add(playername);
			this.getScores().set("changed",changedList);
			List<String> changedScoreRanks=this.getRanks().getStringList("score");
			changedScoreRanks.add(playername);
			this.getRanks().set("score",changedScoreRanks);
			List<String> changedKdrRanks=this.getScores().getStringList("kdr");
			changedKdrRanks.add(playername);
			this.getRanks().set("kdr",changedKdrRanks);
			List<String> changedKillRanks=this.getScores().getStringList("kills");
			changedKillRanks.add(playername);
			this.getRanks().set("kills",changedKillRanks);
			this.saveScores();
			this.saveRanks();
		}
	}
	public void updateRanks(){
		this.getLogger().info("Updating score ranking. This may take a while.");
		List<String> changedPlayerNames=this.getScores().getStringList("changed");
		List<String> rankOrder=this.getRanks().getStringList("score");
		/* Made the rank list into an array over here to make it easier to handle.
		 * 
		 * 
		 * */
		for(int a=0;a<=changedPlayerNames.size()-1;a++){
			int listSize=this.getRanks().getStringList("score").size();
			String playerName=changedPlayerNames.get(a);
			int oldScore=this.getScores().getInt("players."+playerName+".score");
			double tourneyScore=this.getScores().getDouble("players."+playerName+".tourneyScore");
			int newScore=(int) (oldScore+0.5+changeRate*tourneyScore);
			this.getScores().set("players."+playerName+".score", newScore);
			int newIndex=rankOrder.indexOf(playerName);
			rankOrder.remove(playerName);
			while(newIndex>0&&newScore>this.getScores().getInt("players."+rankOrder.get(newIndex-1)+".score")){
				newIndex--;
			}
			while(newIndex>=0&&newIndex<(listSize-1)&&newScore<this.getScores().getInt("players."+rankOrder.get(newIndex)+".score")){
				newIndex++;//Fix this.
			}
			rankOrder.add(newIndex,playerName);
			/**Update the rankings to the new rankings.
			 * Add that to the code here.
			 * 
			 * This may be useful:
			 * int newWinnerKdrIndex=this.getRanks().getStringList("kdr").indexOf(winnerName);
			List<String> newScoreRank=this.getRanks().getStringList("kdr");
			newWinnerKdrRank.remove(newWinnerKdrIndex);
			while(winnerKdr>this.getScores().getDouble("players."+this.getRanks().getStringList("kdr").get(newWinnerKdrIndex-1)+".kdr")){
				newWinnerKdrIndex--;
			}
			while(winnerKdr<this.getScores().getDouble("players."+this.getRanks().getStringList("kdr").get(newWinnerKdrIndex+1)+".kdr")){
				newWinnerKdrIndex++;
			}
			newWinnerKdrRank.add(newWinnerKdrIndex,winnerName);

			this.getRanks().set("kdr", newWinnerKdrRank);
			 * */


			this.getScores().set("players."+playerName+".tourneyScore", 0.0);
		}
		this.getRanks().set("score", rankOrder);
		/*Sort The normal Rank Array.*/
		this.getScores().set("changed", null);
		this.getLogger().info("Pvp ranks have been updated.");
	}
	public static double round(double input, int decimals){
		for(int a=0;a<=decimals-1;a++){
			input=input*10;
		}
		input=(int) (input+0.5);
		for(int b=0;b<=decimals-1;b++){
			input=input/10;
		}
		return input;
	}
	public static String formatToLength(int string, int length){
		String result=""+string;
		while(result.length()<length){
			result="0"+result;
		}
		return result;
	}
	public final static String DIGITS="0123456789";
}