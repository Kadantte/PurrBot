/*
 *  Copyright 2018 - 2021 Andre601
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 *  and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *  
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 *  OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package site.purrbot.bot.util.message;

import ch.qos.logback.classic.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.slf4j.LoggerFactory;
import site.purrbot.bot.PurrBot;

import java.awt.*;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private final PurrBot bot;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("dd. MMM yyyy HH:mm:ss");
    private final Logger logger = (Logger)LoggerFactory.getLogger("MessageUtil");
    
    private final Pattern placeholder = Pattern.compile("(\\{(.+?)})", Pattern.CASE_INSENSITIVE);
    private final Pattern rolePattern = Pattern.compile("(\\{r_(name|mention):(\\d+)})", Pattern.CASE_INSENSITIVE);
    private final Pattern channelPattern = Pattern.compile("(\\{c_(name|mention):(\\d+)})", Pattern.CASE_INSENSITIVE);
    
    private final DecimalFormat decimalFormat = new DecimalFormat("#,###,###");
    
    private final Cache<String, String> queue = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    public MessageUtil(PurrBot bot){
        this.bot = bot;
    }

    public String getRandomShutdownImg(){
        return bot.getShutdownImg().isEmpty() ? "" : bot.getShutdownImg().get(
                bot.getRandom().nextInt(bot.getShutdownImg().size())
        );
    }

    public String getRandomShutdownMsg(){
        return bot.getShutdownMsg().isEmpty() ? "" : bot.getShutdownMsg().get(
                bot.getRandom().nextInt(bot.getShutdownMsg().size())
        );
    }

    public String getRandomStartupMsg(){
        return bot.getStartupMsg().isEmpty() ? "Starting bot..." : bot.getStartupMsg().get(
                bot.getRandom().nextInt(bot.getStartupMsg().size())
        );
    }

    public String formatTime(LocalDateTime time){
        LocalDateTime utcTime = LocalDateTime.from(time.atOffset(ZoneOffset.UTC));
        return utcTime.format(timeFormat) + " UTC";
    }

    public Color getColor(String input){
        input = input.toLowerCase();
        if(!input.equals("random") && !(input.startsWith("hex:") || input.startsWith("rgb:")))
            return null;
        
        Color color = null;
        
        if(input.equals("random")){
            int r = bot.getRandom().nextInt(256);
            int g = bot.getRandom().nextInt(256);
            int b = bot.getRandom().nextInt(256);
    
            return new Color(r, g, b);
        }
        
        String[] split = input.split(":");
        if(split.length <= 1)
            return null;
        
        String value = split[1];
        
        switch(split[0]){
            case "hex":
                if(value.isEmpty())
                    return null;
                
                color = Color.decode(value.startsWith("#") ? value : "#" + value);
                break;
            
            case "rgb":
                if(value.isEmpty())
                    return null;
                
                String[] rgb = Arrays.copyOf(value.split(","), 3);
                
                try{
                    color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                }catch(Exception ignored){
                    return null;
                }
        }
        
        return color;
    }

    public void sendWelcomeMsg(TextChannel tc, String message, Member member, InputStream file){
        Guild guild = member.getGuild();
    
        message = formatPlaceholders(message, member);
        
        if(file == null || !guild.getSelfMember().hasPermission(tc, Permission.MESSAGE_ATTACH_FILES)){
            tc.sendMessage(message).queue();
            return;
        }
        
        tc.sendMessage(message)
          .addFile(file, String.format(
                  "welcome_%s.jpg",
                  member.getId()
          ))
          .queue();
    }
    
    public String formatPlaceholders(String msg, Member member){
        Guild guild = member.getGuild();
        
        Matcher roleMatcher = rolePattern.matcher(msg);
        if(roleMatcher.find()){
            StringBuilder builder = new StringBuilder();
            do{
                Role role = guild.getRoleById(roleMatcher.group(3));
                if(role == null)
                    continue;
                
                switch(roleMatcher.group(2).toLowerCase()){
                    case "name":
                        roleMatcher.appendReplacement(builder, role.getName());
                        break;
                    
                    case "mention":
                        roleMatcher.appendReplacement(builder, role.getAsMention());
                        break;
                }
            }while(roleMatcher.find());
            
            roleMatcher.appendTail(builder);
            msg = builder.toString();
        }
        
        Matcher channelMatcher = channelPattern.matcher(msg);
        if(channelMatcher.find()){
            StringBuilder builder = new StringBuilder();
            do{
                GuildChannel channel = guild.getGuildChannelById(channelMatcher.group(3));
                if(channel == null)
                    continue;
                
                switch(channelMatcher.group(2).toLowerCase()){
                    case "name":
                        channelMatcher.appendReplacement(builder, channel.getName());
                        break;
                    
                    case "mention":
                        if(!channel.getType().equals(ChannelType.TEXT))
                            continue;
                        
                        TextChannel tc = guild.getTextChannelById(channelMatcher.group(3));
                        if(tc == null)
                            continue;
                        
                        channelMatcher.appendReplacement(builder, tc.getAsMention());
                        break;
                }
            }while(channelMatcher.find());
            
            channelMatcher.appendTail(builder);
            msg = builder.toString();
        }
        
        Matcher matcher = placeholder.matcher(msg);
        if(matcher.find()){
            StringBuilder builder = new StringBuilder();
            do{
                switch(matcher.group(2).toLowerCase()){
                    case "mention":
                        matcher.appendReplacement(builder, member.getAsMention());
                        break;
                    
                    case "name":
                    case "username":
                        matcher.appendReplacement(builder, member.getEffectiveName());
                        break;
                    
                    case "guild":
                    case "server":
                        matcher.appendReplacement(builder, guild.getName());
                        break;
                    
                    case "count":
                    case "members":
                        matcher.appendReplacement(builder, String.valueOf(guild.getMemberCount()));
                        break;
                    
                    case "count_formatted":
                    case "members_formatted":
                        matcher.appendReplacement(builder, formatNumber(guild.getMemberCount()));
                        break;
                    
                    case "tag":
                        matcher.appendReplacement(builder, member.getUser().getAsTag());
                }
            }while(matcher.find());
            
            matcher.appendTail(builder);
            msg = builder.toString();
        }
        
        return msg;
    }

    public String getBotGame(long guilds){
        String game = bot.isBeta() ? "My sister on %s guilds." : "https://purrbot.site | %s Guilds";
        
        
        return String.format(game, formatNumber(guilds));
    }
    
    public String replaceLast(String input, String target, String replacement){
        if(!input.contains(target))
            return input;
        
        StringBuilder builder = new StringBuilder(input);
        builder.replace(input.lastIndexOf(target), input.lastIndexOf(target) + 1, replacement);
        
        return builder.toString();
    }
    
    public String formatNumber(long number){
        return decimalFormat.format(number);
    }
    
    public boolean hasArg(String value, String... args){
        if(args.length == 0)
            return false;
        
        for(String arg : args){
            if(arg.equalsIgnoreCase("--" + value))
                return true;
            
            if(arg.equalsIgnoreCase("—" + value))
                return true;
        }
        
        return false;
    }
}
