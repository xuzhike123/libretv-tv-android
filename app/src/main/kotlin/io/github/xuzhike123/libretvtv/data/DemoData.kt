package io.github.xuzhike123.libretvtv.data

/**
 * 占位数据：所有片名、来源、延迟、EPG 节目均为演示用虚构内容，
 * 不代表任何真实检索结果。接入真实 LibreTV 实例后，由网络层替换本文件。
 */
private val TITLES = listOf(
    "夜行列车", "雾港往事", "山海之间", "无声证词", "城市边缘", "拾光者",
    "归途漫漫", "荒原来信", "第十二号档案", "春分之后", "铁轨尽头", "白鸟栖息地",
    "逆流而上", "旧城记忆", "星野调查组", "第七个夏天", "长夜将尽", "浮岛书店",
    "追风纪事", "灰度地带", "江畔人家", "候鸟计划", "午夜剧场", "纸上山河"
)

private val SHOWS = listOf(
    "山海令", "长安旧事录", "雾中列车", "北街十二号",
    "远山淡影", "拾光小馆", "春分之前", "铁马冰河"
)

private val SOURCE_NAMES = listOf("示例源 A", "示例源 B", "示例源 C", "示例源 D", "示例源 E")

private fun titleAt(index: Int): String = TITLES[index % TITLES.size]
private fun sourceAt(index: Int): String = SOURCE_NAMES[index % SOURCE_NAMES.size]
private fun durationFor(index: Int): String {
    val minutes = 8 + (index * 7) % 50
    val seconds = (index * 13) % 60
    return "%d:%02d".format(minutes, seconds)
}

fun homeRows(): List<ContentRow> = listOf(
    ContentRow(
        title = "继续观看",
        subtitle = "本机进度，秒级续播",
        items = List(8) { i ->
            VideoItem(
                id = "resume-$i",
                title = titleAt(i + 3),
                source = sourceAt(i),
                duration = durationFor(i + 3),
                secondaryLine = "上次播放 12:4$i",
                progressPercent = 18 + (i * 11) % 65
            )
        }
    ),
    ContentRow(
        title = "豆瓣电影推荐",
        subtitle = "可在设置中切换推荐来源",
        items = List(10) { i ->
            VideoItem(
                id = "movie-$i",
                title = titleAt(i + 1),
                source = sourceAt(i),
                duration = durationFor(i + 1),
                secondaryLine = "豆瓣 7.${(i * 7) % 9}"
            )
        }
    ),
    ContentRow(
        title = "豆瓣剧集推荐",
        subtitle = "按更新时间排序",
        items = List(8) { i ->
            VideoItem(
                id = "tv-$i",
                title = SHOWS[i],
                source = sourceAt(i),
                duration = durationFor(i + 2),
                secondaryLine = "更新至第 ${12 + i} 集"
            )
        }
    ),
    ContentRow(
        title = "新番放送",
        subtitle = "按放送时间表排序",
        items = List(8) { i ->
            VideoItem(
                id = "anime-$i",
                title = "${SHOWS[(i + 2) % SHOWS.size]} 第 ${i + 1} 话",
                source = sourceAt(i + 1),
                duration = durationFor(i + 5),
                secondaryLine = "每周三更新"
            )
        }
    ),
    ContentRow(
        title = "热播榜单",
        subtitle = "影视榜单聚合",
        items = List(10) { i ->
            VideoItem(
                id = "hot-$i",
                title = titleAt(i + 9),
                source = sourceAt(i + 2),
                duration = durationFor(i + 7),
                secondaryLine = "热度 ${98 - i}"
            )
        }
    ),
    ContentRow(
        title = "我的收藏",
        subtitle = "仅保存在本机",
        items = List(6) { i ->
            VideoItem(
                id = "fav-$i",
                title = titleAt(i + 15),
                source = sourceAt(i),
                duration = durationFor(i + 11),
                secondaryLine = "已缓存"
            )
        }
    )
)

fun favoriteRows(): List<ContentRow> = homeRows().filter { it.title == "我的收藏" }

fun historyRows(): List<ContentRow> = listOf(
    ContentRow(
        title = "观看历史",
        subtitle = "近 7 天",
        items = List(12) { i ->
            VideoItem(
                id = "history-$i",
                title = titleAt(i + 6),
                source = sourceAt(i + 1),
                duration = durationFor(i + 4),
                secondaryLine = "观看于 ${i + 1} 天前",
                progressPercent = 40 + (i * 13) % 60
            )
        }
    )
)

fun sourceItems(): List<SourceItem> {
    val addresses = listOf(
        "api.example-a.test",
        "api.example-b.test",
        "cdn.example-c.test",
        "m3u.example-d.test"
    )
    return SOURCE_NAMES.mapIndexed { index, name ->
        SourceItem(
            name = name,
            address = addresses[index % addresses.size],
            latencyMs = 60 + (index * 37) % 210,
            healthy = index != 4
        )
    }
}

fun liveGroups(): List<String> = listOf("央视", "卫视", "地方", "体育", "影视", "少儿", "纪实")

fun liveChannels(): List<ChannelItem> {
    val programs = listOf(
        "早间新闻", "纪录片：河流的尽头", "电视剧连播",
        "体育赛事直播", "晚间剧场", "电影时段", "少儿节目"
    )
    val groups = liveGroups()
    return List(16) { i ->
        ChannelItem(
            number = "CH${i + 1}",
            name = "${groups[i % groups.size]}频道 ${i + 1}",
            nowPlaying = programs[i % programs.size],
            next = programs[(i + 3) % programs.size]
        )
    }
}

fun searchHistory(): List<String> = listOf("山海", "实验片", "单集 20 分钟", "高分剧", "治愈系")

fun searchHotWords(): List<String> =
    listOf("周末看什么", "高分悬疑", "国创动画", "老片修复", "纪录片推荐")

fun findVideo(videoId: String): VideoItem =
    (homeRows() + favoriteRows() + historyRows())
        .flatMap { it.items }
        .firstOrNull { it.id == videoId }
        ?: VideoItem(
            id = videoId,
            title = "未找到该条目",
            source = sourceAt(0),
            duration = durationFor(0)
        )
