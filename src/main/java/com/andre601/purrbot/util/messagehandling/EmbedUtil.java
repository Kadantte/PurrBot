package com.andre601.purrbot.util.messagehandling;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;

import java.awt.*;
import java.time.ZonedDateTime;

public class EmbedUtil {

    /**
     * Gives a Embed with a set footer.
     * <br>It uses {@link #getEmbed()} to get a default EmbedBuilder with a set color.
     *
     * @param  user
     *         The {@link net.dv8tion.jda.core.entities.User User object} used for the footer.
     *
     * @return A {@link net.dv8tion.jda.core.EmbedBuilder EmbedBuilder} with a set footer and timestamp.
     */
    public static EmbedBuilder getEmbed(User user){
        return getEmbed().setFooter(String.format(
                "Requested by: %s",
                MessageUtil.getTag(user)
        ), user.getEffectiveAvatarUrl()).setTimestamp(ZonedDateTime.now());
    }

    /**
     * Gives a Embed with a set footer.
     *
     * @return A {@link net.dv8tion.jda.core.EmbedBuilder EmbedBuilder} with a set color.
     */
    public static EmbedBuilder getEmbed(){
        return new EmbedBuilder().setColor(new Color(54, 57, 63));
    }

    /**
     * Sends a {@link net.dv8tion.jda.core.entities.MessageEmbed MessageEmbed} to the provided channel.
     * <br>If the text in {@param msg} is to big for one Embed, then the text will be cut, the embed send and the
     * action repeated with the remaining text.
     *
     * @param tc
     *        A {@link net.dv8tion.jda.core.entities.TextChannel TextChannel}.
     * @param msg
     *        A {@link java.lang.String String}.
     * @param footer
     *        A {@link java.lang.String String} to set the text in the Embed-footer.
     * @param color
     *        A {@link java.awt.Color Color} to set the embed-color.
     */
    public static void sendEvalEmbed(TextChannel tc, String msg, String footer, Color color){
        String newMsg = msg;

        String overflow = null;
        if (newMsg.length() > 2000){
            overflow = newMsg.substring(1999);
            newMsg = newMsg.substring(0, 1999);
        }

        EmbedBuilder message = getEmbed()
                .setColor(color)
                .setDescription(newMsg)
                .setFooter(footer, null)
                .setTimestamp(ZonedDateTime.now());

        tc.sendMessage(message.build()).queue();
        if(overflow != null)
            sendEvalEmbed(tc, overflow, footer, color);
    }

    /**
     * Sends a embed with a message to the provided channel.
     *
     * @param msg
     *        A {@link net.dv8tion.jda.core.entities.Message Message object} to get the channel.
     * @param error
     *        A {@link java.lang.String String} that will be used in the description.
     */
    public static void error(Message msg, String error){
        EmbedBuilder errorEmbed = getEmbed(msg.getAuthor()).setColor(Color.RED).setDescription(error);

        msg.getTextChannel().sendMessage(errorEmbed.build()).queue();
    }

    /**
     * Sends a embed with a message to the provided channel.
     *
     * @param tc
     *        A {@link net.dv8tion.jda.core.entities.TextChannel TextChannel object} to get the channel.
     * @param error
     *        A {@link java.lang.String String} that will be used in the description.
     */
    public static void error(TextChannel tc, String error){
        EmbedBuilder errorEmbed = getEmbed().setColor(Color.RED).setDescription(error);

        tc.sendMessage(errorEmbed.build()).queue();
    }
}
