package org.miowing.mioverify.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

/**
 * <h1>材质信息</h1>
 * 序列化后的 Json 格式，不对应数据库
 * <pre>
 * {
 * 	"timestamp":该属性值被生成时的时间戳（Java 时间戳格式，即自 1970-01-01 00:00:00 UTC 至今经过的毫秒数）,
 * 	"profileId":"角色 UUID（无符号）",
 * 	"profileName":"角色名称",
 * 	"textures":{ // 角色的材质
 * 		"材质类型（如 SKIN）":{ // 若角色不具有该项材质，则不必包含
 * 			"url":"材质的 URL",
 * 			"metadata":{ // 材质的元数据，若没有则不必包含
 * 				"名称":"值"
 * 				// ,...（可以有更多）
 *            }
 *        }
 * 		// ,...（可以有更多）
 *    }
 * }
 * </pre>
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TexturesShow {
    private Long timestamp;
    private String profileId;
    private String profileName;
    private @Nullable Texture skin;
    private @Nullable Texture cape;
    @Data
    @Accessors(chain = true)
    public static class Texture {
        private String url;
        private Metadata metadata;
    }
    @Data
    @Accessors(chain = true)
    public static class Metadata {
        private String model;
    }
}