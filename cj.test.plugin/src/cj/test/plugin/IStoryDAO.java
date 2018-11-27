package cj.test.plugin;

import org.apache.ibatis.annotations.Insert;

public interface IStoryDAO {
	@Insert("insert into story(id,title) values(#{id},#{title})")
	void save(StoryBO bo);
}
