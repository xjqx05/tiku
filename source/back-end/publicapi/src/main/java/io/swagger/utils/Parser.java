package io.swagger.utils;

import io.swagger.model.Expression;
import io.swagger.model.QuerryInfo;
import io.swagger.pojo.PaperFullData;
import io.swagger.pojo.dao.*;
import io.swagger.pojo.dao.repos.*;
import io.swagger.service.PaperDataService;
import io.swagger.service.ProblemDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询表达式解析器
 *
 * @see io.swagger.model.Expression
 */
@Service
@Slf4j
public class Parser {


    /**
     * 根据传进来的查询表达式对象查询所有符合表达式的问题
     *
     * @param expression
     * @return
     */
    public @NotNull List<Long> getAllProblemsByExpression(QuerryInfo expression) throws ParserErrorException {
        List<Long> longs = executeExpression(expression.getQuerry(), false);

        return longs;
    }


    /**
     * 根据传进来的查询表达式对象查询所有符合表达式的试卷
     *
     * @param expression
     * @return
     */
    public List<PaperFullData> getAllPapersByExpression(Expression expression, boolean isDeep) throws ParserErrorException {
        List<Long> longs = executeExpression(expression, true);

        return paperDataService.getFullDataByIds(longs, isDeep);
    }


    /**
     * 查询数据项为该值的所有问题id
     * 数据项定义在这里https://www.yuque.com/czfshine/olm1pa/as07ca
     *
     * @param fieldName
     * @param value
     * @return
     */
    private List<Long> problemEquals(@NotNull String fieldName, @NotNull String value) throws ParserErrorException {
        ArrayList<Long> res = new ArrayList<>();
        try {

            switch (fieldName) {
                case "problemId": {
                    //todo 捕获数字解析错误
                    Optional<Problem> byId = problemRepository.findById(Long.valueOf(value));
                    byId.ifPresent(problem -> res.add(problem.getId()));
                    break;
                }
                default:
                    throw new ParserErrorException("域[" + fieldName + "]不支持[==]操作");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParserErrorException("未知错误:" + e.getClass());
        }
        return res;
    }

    /**
     * 执行包含操作
     *
     * @param fieldName
     * @param strings
     * @return
     */
    private List<Long> problemContains(String fieldName, List<String> strings) throws ParserErrorException {
        switch (fieldName) {
            case "tags": {
                return problemContainsTags(strings);
            }
            default:
                throw new ParserErrorException("域[" + fieldName + "]不支持[contains]操作");
        }
    }

    /**
     * 找出含有所有标签的所有问题id
     *
     * @param tags
     * @return
     */
    private List<Long> problemContainsTags(List<String> tags) {
        ArrayList<Long> res = new ArrayList<>();

        //每个问题出现对应标签的数量，结果应该是数量和输入的数量一样
        HashMap<Long, Integer> tagCount = new HashMap<>();

        for (String tagname : tags
        ) {

            TryGetTag tryGetTag = new TryGetTag(tagname).invoke();
            if (tryGetTag.is()) return new ArrayList<>();
            Long id = tryGetTag.getId();
            List<ProblemTag> pts = problemTagRepository.findAllByTagIdEquals(id);
            pts.forEach((pt) -> tagCount.merge(pt.getProblemId(), 1, (n, o) -> n + o));

        }

        applyMap(tags, res, tagCount);
        return res;
    }

    private void applyMap(List<String> tags, ArrayList<Long> res, HashMap<Long, Integer> tagCount) {
        int len = tags.size();
        tagCount.forEach((k, v) -> {
            if (v == len) {
                res.add(k);
            }
        });
    }

    /**
     * 查询数据项为该值的所有问题id
     * 数据项定义在这里https://www.yuque.com/czfshine/olm1pa/as07ca
     *
     * @param fieldName
     * @param value
     * @return
     */
    private List<Long> paperEquals(String fieldName, String value) throws ParserErrorException {
        ArrayList<Long> res = new ArrayList<>();
        try {

            switch (fieldName) {
                case "paperId":
                case "collectionId": {
                    //todo 捕获数字解析错误
                    Optional<Paper> byId = paperRepository.findById(Long.valueOf(value));
                    byId.ifPresent(problem -> res.add(problem.getId()));
                    break;
                }
                default:
                    throw new ParserErrorException("域[" + fieldName + "]不支持[==]操作,或者域未定义在规范里面");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ParserErrorException("未知错误:" + e.getClass() + ":" + e.getMessage());
        }
        return res;
    }

    /**
     * 执行包含操作
     *
     * @param fieldName
     * @param strings
     * @return
     */
    private List<Long> paperContains(String fieldName, List<String> strings) throws ParserErrorException {
        switch (fieldName) {
            case "tags": {
                return paperContainsTags(strings);
            }
            default:
                throw new ParserErrorException("域[" + fieldName + "]不支持[contains]操作 ");
        }
    }

    /**
     * 找出含有所有标签的所有问题id
     *
     * @param tags
     * @return
     */
    private List<Long> paperContainsTags(List<String> tags) {
        ArrayList<Long> res = new ArrayList<>();

        //每个问题出现对应标签的数量，结果应该是数量和输入的数量一样
        HashMap<Long, Integer> tagCount = new HashMap<>();

        for (String tagname : tags
        ) {
            TryGetTag tryGetTag = new TryGetTag(tagname).invoke();
            if (tryGetTag.is()) return new ArrayList<>();

            Long id = tryGetTag.getId();
            List<PaperTag> pts = paperTagRepository.findAllByTagIdEquals(id);
            pts.forEach((pt) -> tagCount.merge(pt.getPaperId(), 1, (n, o) -> n + o));

        }

        applyMap(tags, res, tagCount);
        return res;
    }

    /**
     * 执行表达式，返回符合结果的问题id
     *
     * @param expression
     * @return
     * @throws ParserErrorException
     */
    private @NotNull List<Long> executeExpression(Expression expression, boolean isPaper) throws ParserErrorException {
        @NotNull String op = expression.getOperator();


        switch (op) {
            case "==": {
                @NotNull Object argument1 = expression.getArgument1();
                @NotNull Object argument2 = expression.getArgument2();
                checkEqualsArgument(expression, argument1, argument2);
                List<Long> res;
                if (isPaper) {
                    res = paperEquals((String) argument1, (String) argument2);
                } else {
                    res = problemEquals((String) argument1, (String) argument2);
                }
                return res;
            }
            case "contains": {
                @NotNull Object argument1 = expression.getArgument1();
                @NotNull Object argument2 = expression.getArgument2();
                checkContainsArgument(expression, argument1, argument2);

                ArrayList<String> strings = new ArrayList<>();
                for (int i = 0; i < ((List) argument2).size(); i++) {
                    Object item = ((List) argument2).get(i);
                    if (!(item instanceof String)) {
                        throw new ParserErrorException("操作【contains】的第二个参数数组元素必须是字符串，但是收到的不是");
                    }
                    strings.add((String) item);
                }
                List<Long> res;
                if (isPaper) {
                    res = paperContains((String) argument1, strings);
                } else {
                    res = problemContains((String) argument1, strings);
                }
                return res;
            }
            case "and": {
                GetLogicArgument getLogicArgument = new GetLogicArgument(expression).invoke();
                Expression expression1 = getLogicArgument.getExpression1();
                Expression expression2 = getLogicArgument.getExpression2();

                return andOperator(expression1, expression2, isPaper);
            }
            case "or": {
                GetLogicArgument getLogicArgument = new GetLogicArgument(expression).invoke();
                Expression expression1 = getLogicArgument.getExpression1();
                Expression expression2 = getLogicArgument.getExpression2();
                return orOperator(expression1, expression2, isPaper);
            }
            default:
                throw new ParserErrorException("操作【" + op + "】目前不被支持");
        }

    }

    private void checkContainsArgument(Expression expression, @NotNull Object argument1, @NotNull Object argument2) throws ParserErrorException {
        if (!(argument1 instanceof String)) {
            throw new ParserErrorException("操作【" + expression.getOperator() + "】的第一个参数类型必须是字符串，但是收到的不是");
        }

        if (!(argument2 instanceof List)) {
            throw new ParserErrorException("操作【contains】的第二个参数类型必须是数组，但是收到的不是");
        }
        int size = ((List) argument2).size();
        if (size == 0) {
            throw new ParserErrorException("操作【contains】的第二个参数数组为空");
        }
    }

    private void checkEqualsArgument(Expression expression, @NotNull Object argument1, @NotNull Object argument2) throws ParserErrorException {
        if (!(argument1 instanceof String)) {
            throw new ParserErrorException("操作【" + expression.getOperator() + "】的第一个参数类型必须是字符串，但是收到的不是");
        }
        if (!(argument2 instanceof String)) {
            throw new ParserErrorException("操作【==】的第二个参数类型必须是字符串，但是收到的不是");
        }
    }


    private Expression mapToExpression(LinkedHashMap json) throws ParserErrorException {
        Expression expression = new Expression();
        Object operator;
        Object argument1;
        Object argument2;

        try {

            operator = json.getOrDefault("operator", null);

            if (operator == null) {
                throw new ParserErrorException("表示式的json对象必须有operator域");
            }

            if (!(operator instanceof String)) {
                throw new ParserErrorException("表示式的json对象的operator域必须是字符串");
            }

            argument1 = json.getOrDefault("argument1", null);

            if (argument1 == null) {
                throw new ParserErrorException("表示式的json对象必须有argument1域");
            }
            argument2 = json.getOrDefault("argument2", null);

            if (argument2 == null) {
                throw new ParserErrorException("表示式的json对象必须有argument2域");
            }

        } catch (Exception e) {
            throw new ParserErrorException("解析表达式对象失败");
        }
        expression.setOperator((String) operator);
        expression.setArgument1(argument1);
        expression.setArgument2(argument2);
        return expression;
    }

    private List<Long> orOperator(Expression left, Expression right, boolean isPaper) throws ParserErrorException {
        // todo 短路
        List<Long> l = executeExpression(left, isPaper);
        List<Long> r = executeExpression(right, isPaper);
        HashSet<Long> ls = new HashSet<>(l);
        ls.addAll(r);
        return new ArrayList<>(ls);
    }

    private List<Long> andOperator(Expression left, Expression right, boolean isPaper) throws ParserErrorException {
        // todo 短路
        List<Long> l = executeExpression(left, isPaper);
        List<Long> r = executeExpression(right, isPaper);
        HashSet<Long> ls = new HashSet<>(l);
        return r.stream().filter(ls::contains).collect(Collectors.toList());
    }

    @Autowired
    private ProblemRepository problemRepository;
    @Autowired
    private ProblemTagRepository problemTagRepository;
    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProblemDataService problemDataService;
    @Autowired

    private PaperDataService paperDataService;
    @Autowired
    private PaperTagRepository paperTagRepository;
    @Autowired
    private PaperRepository paperRepository;

    private class TryGetTag {
        private boolean myResult;
        private String tagname;
        private Long id;

        public TryGetTag(String tagname) {
            this.tagname = tagname;
        }

        boolean is() {
            return myResult;
        }

        public Long getId() {
            return id;
        }

        public TryGetTag invoke() {
            List<Tag> byValueEquals = tagRepository.findByValueEquals(tagname);
            if (byValueEquals == null || byValueEquals.size() == 0) {
                // 找不到这个标签
                log.warn("前端请求标签{},但是数据库没有这个标签的记录", tagname);
                //所有标签是与的关系，一个没有就没有结果
                myResult = true;
                return this;
            }

            id = byValueEquals.get(0).getId();
            myResult = false;
            return this;
        }
    }

    private class GetLogicArgument {
        private Expression expression;
        private Expression expression1;
        private Expression expression2;

        public GetLogicArgument(Expression expression) {
            this.expression = expression;
        }

        public Expression getExpression1() {
            return expression1;
        }

        public Expression getExpression2() {
            return expression2;
        }

        public GetLogicArgument invoke() throws ParserErrorException {
            @NotNull Object argument1 = expression.getArgument1();
            @NotNull Object argument2 = expression.getArgument2();

            if (!(argument1 instanceof LinkedHashMap)) {
                throw new ParserErrorException("操作【" + expression.getOperator() + "】的第一个参数类型必须是json对象，但是收到的不是");
            }

            if (!(argument2 instanceof LinkedHashMap)) {
                throw new ParserErrorException("操作【" + expression.getOperator() + "】的第二个参数类型必须是json对象，但是收到的不是");
            }
            expression1 = mapToExpression((LinkedHashMap) argument1);
            expression2 = mapToExpression((LinkedHashMap) argument2);
            return this;
        }
    }
}

