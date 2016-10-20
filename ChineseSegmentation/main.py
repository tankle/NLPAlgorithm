# -*- coding: utf-8 -*-
#
# @author hztancong
#
# 基本的分词算法


import codecs


def load_dict(file_name):
    """
    加载词典
    :param file_name:
    :return:
    """
    f = codecs.open(file_name, "r", "UTF-8")
    words = []
    for line in f:
        line = line.strip()
        if len(line) > 0:
            words.append(line)
    f.close()

    max_len = 0
    for word in words:
        if len(word) > max_len:
            max_len = len(word)

    return words, max_len


def forward_seg(query, max_len, dicts):
    """
    正向最大匹配分词算法
    :param query:   带分词的串
    :param max_len: 最大匹配长度
    :param dicts:   字典文件
    :return:
    """

    word_list = []

    while len(query) > 0:
        word = query[:max_len]

        is_meet = False  # 标记是否可以分词

        while not is_meet and len(word) > 0:
            # 词典中包含
            if word in dicts:
                word_list.append(word)
                query = query[len(word):]
                is_meet = True
            else:  # 不包含
                # 当词长度为1 的时候，直接当做一个词
                if len(word) == 1:
                    word_list.append(word)
                    query = query[1:]
                    is_meet = True
                else:
                    word = word[:-1]

    # 返回切分结果
    return word_list


def backward_seg(query, max_len, dicts):
    """
    逆向最大匹配分词算法
    :param query:   带分词的串
    :param max_len: 最大匹配长度
    :param dicts:   字典文件
    :return:
    """

    word_list = []

    while len(query) > 0:
        word = query[-max_len: len(query)]

        is_meet = False  # 标记是否可以分词

        while not is_meet and len(word) > 0:
            # 词典中包含
            if word in dicts:
                word_list.insert(0, word)
                query = query[:len(query) - len(word)]
                is_meet = True
            else:  # 不包含
                # 当词长度为1 的时候，直接当做一个词
                if len(word) == 1:
                    word_list.insert(0, word)
                    query = query[:-1]
                    is_meet = True
                else:
                    word = word[1:]

    # 返回切分结果
    return word_list


def out_result(query, dicts, max_len, func):
    """
    输出结果
    :param query:
    :param dicts:
    :param max_len:
    :return:
    """
    words = func(query, max_len, dicts)
    for word in words:
        print word, "/",
    print "\n"


if __name__ == "__main__":
    dict_name = "dict.txt"
    dicts, max_len = load_dict(dict_name)

    query = u"南京市长江大桥"
    out_result(query, dicts, max_len, forward_seg)
    out_result(query, dicts, max_len, backward_seg)
    query = u"我们在野生动物园玩"
    out_result(query, dicts, max_len, forward_seg)
    out_result(query, dicts, max_len, backward_seg)

    query = u"长春药店"
    out_result(query, dicts, max_len, forward_seg)
    out_result(query, dicts, max_len, backward_seg)
