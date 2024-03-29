package com.hiroshi.cimoc.parser;

import com.hiroshi.cimoc.model.Comic;

import java.util.regex.Matcher;

/**
 * Created by Hiroshi on 2016/9/21.
 */

public abstract class RegexIterator implements SearchIterator {

    private Matcher match;

    protected RegexIterator(Matcher match) {
        this.match = match.find()?match:null;
        match.reset();
    }

    @Override
    public boolean hasNext() {
        return  match.find();
    }

    @Override
    public Comic next() {
        return parse(match);
    }

    @Override
    public boolean empty() {
        return false;
    }

    protected abstract Comic parse(Matcher match);

}
