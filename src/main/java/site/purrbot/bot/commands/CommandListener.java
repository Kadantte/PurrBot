package site.purrbot.bot.commands;

import ch.qos.logback.classic.Logger;
import com.github.rainestormee.jdacommand.CommandHandler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;
import site.purrbot.bot.PurrBot;
import site.purrbot.bot.constants.Emotes;
import site.purrbot.bot.constants.IDs;
import site.purrbot.bot.constants.Links;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CommandListener extends ListenerAdapter{

    private Logger logger = (Logger)LoggerFactory.getLogger(CommandListener.class);

    private final ThreadGroup CMD_THREAD = new ThreadGroup("CommandThread");
    private final Executor CMD_EXECUTOR = Executors.newCachedThreadPool(
            r -> new Thread(CMD_THREAD, r, "CommandPool")
    );

    private PurrBot manager;
    private final CommandHandler HANDLER;

    public CommandListener(PurrBot manager, CommandHandler handler){
        this.manager = manager;
        this.HANDLER = handler;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event){

        CMD_EXECUTOR.execute(
                () -> {
                    Message msg = event.getMessage();
                    Guild guild = event.getGuild();
                    User user = event.getAuthor();

                    if(user.isBot()) return;

                    String prefix = manager.getPrefixes()
                            .get(guild.getId(), k -> manager.getDbUtil().getPrefix(guild.getId()));

                    String raw = msg.getContentRaw().toLowerCase();

                    String userMention = "<@" + event.getJDA().getSelfUser().getId() + ">";
                    String memberMention = "<@!" + event.getJDA().getSelfUser().getId() + ">";

                    if(!raw.startsWith(prefix) && !raw.startsWith(userMention) && !raw.startsWith(memberMention)){
                        if(guild.getId().equals(IDs.GUILD.getId()))
                            manager.getLevelManager().giveXP(user.getId(), false, msg.getTextChannel());

                        return;
                    }

                    TextChannel tc = event.getChannel();

                    if(!manager.getPermUtil().hasPermission(tc, Permission.MESSAGE_WRITE))
                        return;

                    if(raw.equalsIgnoreCase(userMention) || raw.equalsIgnoreCase(memberMention)){
                        tc.sendMessage(String.format(
                                "Hey there %s!\n" +
                                "My prefix on this Discord is `%s`\n" +
                                "Run `%shelp` for a list of commands.",
                                msg.getMember().getAsMention(),
                                prefix,
                                prefix
                        )).queue();
                        return;
                    }

                    String[] args = null;
                    String cmdString = null;

                    if(raw.startsWith(userMention) || raw.startsWith(memberMention)) {
                        args = split(raw, raw.indexOf(">") + 1);
                        cmdString = args[0];
                    }

                    if((args == null || cmdString == null) && raw.toLowerCase().startsWith(prefix)) {
                        args = split(raw, prefix.length());
                        cmdString = args[0];
                    }

                    if(cmdString == null) return;

                    Command command = (Command)HANDLER.findCommand(cmdString.toLowerCase());

                    if(command == null) return;
                    if(command.getAttribute("category").equals("owner") &&
                            !user.getId().equals(IDs.ANDRE_601.getId())) return;

                    if(!manager.getPermUtil().hasPermission(tc, Permission.MESSAGE_EMBED_LINKS)){
                        tc.sendMessage(String.format(
                                "I need permission to embed links in this channel %s!",
                                msg.getMember().getAsMention()
                        )).queue();
                        return;
                    }
                    if(!manager.getPermUtil().hasPermission(tc, Permission.MESSAGE_ADD_REACTION)){
                        manager.getEmbedUtil().sendError(tc, user, "I need permission to add reactions!");
                        return;
                    }
                    if(command.getAttribute("category").equals("nsfw") && !tc.isNSFW()){
                        manager.getEmbedUtil().sendError(tc, msg.getAuthor(), String.format(
                                manager.getMessageUtil().getRandomNoNsfwMsg(),
                                msg.getMember().getEffectiveName()
                        ));
                        return;
                    }
                    if(command.hasAttribute("manage_server")){
                        if(!manager.getPermUtil().hasPermission(tc, msg.getMember(), Permission.MANAGE_SERVER)){
                            manager.getEmbedUtil().sendError(tc, user, "You need the `manage server` permission!");
                            return;
                        }
                    }
                    if(command.hasAttribute("guild_only") && !guild.getId().equals(IDs.GUILD.getId())){
                        manager.getEmbedUtil().sendError(tc, user, String.format(
                                "This command can only be used in [my Discord](%s)!",
                                Links.DISCORD.getUrl()
                        ));
                        return;
                    }

                    try{
                        //noinspection unchecked
                        HANDLER.execute(command, msg, args[1] == null ? "" : args[1]);

                        if(guild.getId().equals(IDs.GUILD.getId()))
                            manager.getLevelManager().giveXP(user.getId(), true, tc);
                    }catch(Exception ex){
                        logger.error("Couldn't perform command!", ex);
                        manager.getEmbedUtil().sendError(tc, user, String.format(
                                "Uhm... This is actually a bit embarrassing, but I had an error with a command. %s\n" +
                                "Please [join my Discord](%s) or report the issue on [GitHub](%s)!",
                                Emotes.VANILLABLUSH.getEmote(),
                                Links.DISCORD.getUrl(),
                                Links.GITHUB.getUrl()
                        ), ex.getMessage());
                    }
                }
        );
    }

    private String[] split(String raw, int length){
        return Arrays.copyOf(raw.substring(length).trim().split("\\s+", 2), 2);
    }
}