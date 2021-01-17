package me.mrgeneralq.sleepmost.eventlisteners;

import static me.mrgeneralq.sleepmost.enums.SleepSkipCause.NIGHT_TIME;

import me.mrgeneralq.sleepmost.flags.NightcycleAnimationFlag;
import me.mrgeneralq.sleepmost.interfaces.IFlagsRepository;
import me.mrgeneralq.sleepmost.interfaces.ISleepService;
import me.mrgeneralq.sleepmost.statics.ServerVersion;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.mrgeneralq.sleepmost.enums.SleepSkipCause;
import me.mrgeneralq.sleepmost.events.SleepSkipEvent;
import me.mrgeneralq.sleepmost.interfaces.IConfigService;
import me.mrgeneralq.sleepmost.interfaces.IMessageService;
import me.mrgeneralq.sleepmost.statics.DataContainer;

public class SleepSkipEventListener implements Listener {

	private final IMessageService messageService;
	private final IConfigService configService;
	private final ISleepService sleepService;
	private final DataContainer dataContainer = DataContainer.getContainer();

	public SleepSkipEventListener(IMessageService messageService, IConfigService configService, ISleepService sleepService) {
		this.messageService = messageService;
		this.configService = configService;
		this.sleepService = sleepService;
	}

	@EventHandler
	public void onSleepSkip(SleepSkipEvent e){

		World world = e.getWorld();

		if(dataContainer.animationRunning(world))
			return;
		
		resetPhantomCounter(world);
		sendSkipSound(world, e.getCause());

		if (ServerVersion.CURRENT_VERSION.supportsTitles())
			sendSkipTitle(world, e.getCause());

		this.messageService.sendNightSkippedMessage(e.getWorld(), e.getLastSleeperName(), e.getLastSleeperDisplayName(), e.getCause());
	}

	private void resetPhantomCounter(World world) 
	{
		/*
		 * DISCLAIMER: Statistic and TIME_SINCE_REST Does not exist
		 * in older versions of Minecraft
		 */
		try
		{
			for(Player p: world.getPlayers())
				p.setStatistic(Statistic.TIME_SINCE_REST, 0);
		}
		catch(NoSuchFieldError error) {
			// statistic did not exist yet in some versions
		}
	}
	private void sendSkipTitle(World world, SleepSkipCause cause) 
	{
		boolean titleEnabled = (cause == NIGHT_TIME ? configService.getTitleNightSkippedEnabled() : configService.getTitleStormSkippedEnabled());
		
		if(!titleEnabled)
		{
			return;
		}
		String skippedTitle = (cause == NIGHT_TIME ? configService.getTitleNightSkippedTitle() : configService.getTitleStormSkippedTitle());
		String skippedSubtitle = (cause == NIGHT_TIME ? configService.getTitleNightSkippedSubTitle() : configService.getTitleStormSkippedSubTitle());
		
		for(Player p : world.getPlayers()) {
			String playerTitle = skippedTitle = skippedTitle.replaceAll("%player%", p.getName()).replaceAll("%dplayer%", p.getDisplayName());
			String playerSubtitle = skippedSubtitle.replaceAll("%player%", p.getName()).replaceAll("%dplayer%", p.getDisplayName());
			
			p.sendTitle(playerTitle, playerSubtitle, 10, 70, 20);
		}
	}
	private void sendSkipSound(World world, SleepSkipCause cause) 
	{
		boolean soundEnabled = (cause == NIGHT_TIME ? configService.getSoundNightSkippedEnabled() : configService.getSoundStormSkippedEnabled());
		
		if(!soundEnabled)
		{
			return;
		}
		Sound skipSound = (cause == NIGHT_TIME ? configService.getSoundNightSkippedSound() : configService.getSoundStormSkippedSound());
		
		for(Player p : world.getPlayers()) {
			p.playSound(p.getLocation(), skipSound, 0.4F, 1F);
		}
	}
}