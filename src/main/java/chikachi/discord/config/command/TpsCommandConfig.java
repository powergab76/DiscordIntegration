package chikachi.discord.config.command;

import chikachi.discord.DiscordClient;
import com.google.common.base.Joiner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.DimensionManager;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.LongStream;

public class TpsCommandConfig extends CommandConfig {
    private static final DecimalFormat timeFormatter = new DecimalFormat("########0.000");

    public TpsCommandConfig() {
        super("tps", false);
    }

    private Integer getMinValue(Set<Integer> values) {
        if (values.size() == 0) {
            return 0;
        }

        Integer value = null;
        for (Integer val : values) {
            if (value == null) {
                value = val;
            } else if (val < value) {
                value = val;
            }
        }
        return value;
    }

    private Integer getMaxValue(Set<Integer> values) {
        if (values.size() == 0) {
            return 0;
        }

        Integer value = null;
        for (Integer val : values) {
            if (value == null) {
                value = val;
            } else if (value < val) {
                value = val;
            }
        }
        return value;
    }

    private Integer getMinLength(Collection<String> strings) {
        if (strings.size() == 0) {
            return 0;
        }

        Integer length = null;
        for (String string : strings) {
            int stringLength = string.length();
            if (length == null) {
                length = stringLength;
            } else if (stringLength < length) {
                length = stringLength;
            }
        }
        return length;
    }

    private Integer getMaxLength(Collection<String> strings) {
        if (strings.size() == 0) {
            return 0;
        }

        Integer length = null;
        for (String string : strings) {
            int stringLength = string.length();
            if (length == null) {
                length = stringLength;
            } else if (length < stringLength) {
                length = stringLength;
            }
        }
        return length;
    }

    @Override
    public void execute(List<String> args) {
        MinecraftServer minecraftServer = MinecraftServer.getServer();
        List<String> tpsTimes = new ArrayList<>();

        Integer[] dimensionIds = DimensionManager.getIDs();
        HashMap<Integer, String> dimensionMap = new HashMap<>();

        for (Integer dimensionId : dimensionIds) {
            dimensionMap.put(dimensionId, DimensionManager.getProvider(dimensionId).getDimensionName());
        }

        int maxDimensionIdLength = Math.max(getMinValue(dimensionMap.keySet()).toString().length(), getMaxValue(dimensionMap.keySet()).toString().length());
        int maxDimensionNameLength = Math.max(getMinLength(dimensionMap.values()), getMaxLength(dimensionMap.values()));

        Set<Map.Entry<Integer, String>> entries = dimensionMap.entrySet();

        for (Map.Entry<Integer, String> entry : entries) {
            Integer dimensionId = entry.getKey();
            String dimensionName = entry.getValue();

            String dimensionIdPrefixString = new String(new char[maxDimensionIdLength - dimensionId.toString().length()]).replace("\0", " ");
            String dimensionNamePostfixString = new String(new char[maxDimensionNameLength - dimensionName.length()]).replace("\0", " ");

            double worldTickTime = this.mean(minecraftServer.worldTickTimes.get(dimensionId)) * 1.0E-6D;
            double worldTPS = Math.min(1000.0 / worldTickTime, 20);

            tpsTimes.add(
                    StatCollector.translateToLocalFormatted(
                            "commands.forge.tps.summary",
                            String.format(
                                    "Dim %s%d (%s)%s",
                                    dimensionIdPrefixString,
                                    dimensionId,
                                    dimensionName,
                                    dimensionNamePostfixString
                            ),
                            timeFormatter.format(worldTickTime),
                            timeFormatter.format(worldTPS)
                    )
            );
        }

        double meanTickTime = this.mean(minecraftServer.tickTimeArray) * 1.0E-6D;
        double meanTPS = Math.min(1000.0 / meanTickTime, 20);
        tpsTimes.add(
                StatCollector.translateToLocalFormatted(
                        "commands.forge.tps.summary",
                        String.format(
                                "Overall%s",
                                new String(new char[maxDimensionIdLength + maxDimensionNameLength]).replace("\0", " ")
                        ),
                        timeFormatter.format(meanTickTime),
                        timeFormatter.format(meanTPS)
                )
        );

        DiscordClient.getInstance().sendMessage(
                String.format(
                        "\n```\n%s\n```",
                        Joiner.on("\n").join(tpsTimes)
                ).replace("\\:", ":")
        );
    }

    private long mean(long[] values) {
        return LongStream.of(values).sum() / values.length;
    }
}