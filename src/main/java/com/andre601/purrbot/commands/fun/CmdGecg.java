package com.andre601.purrbot.commands.fun;

import com.andre601.purrbot.commands.Command;
import com.andre601.purrbot.util.HttpUtil;
import com.andre601.purrbot.util.PermUtil;
import com.andre601.purrbot.util.constants.Errors;
import com.andre601.purrbot.util.messagehandling.EmbedUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.text.MessageFormat;


public class CmdGecg implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent e) {
        return false;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent e) {

        TextChannel tc = e.getTextChannel();
        Message msg = e.getMessage();

        if (!PermUtil.canWrite(tc))
            return;

        if(PermUtil.canDeleteMsg(tc))
            msg.delete().queue();

        if(!PermUtil.canSendEmbed(tc)){
            tc.sendMessage(Errors.NO_EMBED).queue();
            if(PermUtil.canReact(tc))
                e.getMessage().addReaction("🚫").queue();

            return;
        }

        String link = HttpUtil.getGecg();
        if(link == null){
            tc.sendMessage(MessageFormat.format(
                    "{0} It looks like, that there's an issue with the API at the moment.",
                    msg.getAuthor().getAsMention()
            )).queue();
            return;
        }

        EmbedBuilder gecg = EmbedUtil.getEmbed(e.getAuthor())
                .setTitle(MessageFormat.format(
                        "{0}",
                        link.replace("https://cdn.nekos.life/gecg/", "")
                ), link)
                .setImage(link);

        tc.sendMessage("Getting a gecg-image...").queue(message -> {
            //  Editing the message to add the image ("should" prevent issues with empty embeds)
            message.editMessage(gecg.build()).queue();
        });
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent e) {

    }

    @Override
    public String help() {
        return null;
    }
}
