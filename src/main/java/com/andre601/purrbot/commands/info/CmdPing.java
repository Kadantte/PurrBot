package com.andre601.purrbot.commands.info;

import com.andre601.purrbot.util.PermUtil;
import com.andre601.purrbot.util.constants.Emotes;
import com.andre601.purrbot.util.messagehandling.MessageUtil;
import com.andre601.purrbot.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.MessageFormat;
import java.time.temporal.ChronoUnit;

public class CmdPing implements Command{

    @Override
    public boolean called(String[] args, MessageReceivedEvent e) {
        return false;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent e) {
        TextChannel tc = e.getTextChannel();
        Message msg = e.getMessage();

        if(!PermUtil.canWrite(tc))
            return;

        if(msg.getContentRaw().contains("-api")){
            tc.sendMessage(MessageFormat.format(
                    "{0}",
                    (PermUtil.canUseCustomEmojis(tc) ? Emotes.LOADING : "") + "Checking ping to Discord-API..."
            )).queue(message -> message.editMessage(
                    MessageFormat.format(MessageUtil.getRandomAPIPingMsg(),
                            msg.getAuthor().getAsMention(),
                            msg.getJDA().getPing()
                    )
            ).queue());
            return;
        }

        tc.sendMessage(MessageFormat.format(
                "{0}",
                (PermUtil.canUseCustomEmojis(tc) ? Emotes.LOADING : "") + "Checking ping..."
        )).queue(message -> message.editMessage(
                MessageFormat.format(MessageUtil.getRandomPingMsg(),
                        msg.getAuthor().getAsMention(),
                        msg.getCreationTime().until(message.getCreationTime(), ChronoUnit.MILLIS)
                )
        ).queue());
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent e) {

    }

    @Override
    public String help() {
        return null;
    }
}
