import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  FlatList,
  RefreshControl,
} from 'react-native';
import { useSelector } from 'react-redux';
import { Search, BookOpen, Heart, Brain, Apple, Baby, Filter, Bookmark, Clock } from 'lucide-react-native';

import { Text } from '../../components/common/Text';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { useDatabase } from '../../database';
import { useTheme } from '../../theme/ThemeContext';
import { ContentArticle } from '../../types';

export const ContentScreen = () => {
  const theme = useTheme();
  const { db } = useDatabase();
  const [articles, setArticles] = useState<ContentArticle[]>([]);
  const [filteredArticles, setFilteredArticles] = useState<ContentArticle[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [refreshing, setRefreshing] = useState(false);
  const { age } = useSelector((state: any) => state.settings.healthProfile);

  useEffect(() => {
    loadArticles();
  }, []);

  useEffect(() => {
    filterArticles();
  }, [searchQuery, selectedCategory, articles]);

  const loadArticles = async () => {
    // In production, this would load from database or API
    const sampleArticles: ContentArticle[] = [
      {
        id: '1',
        title: 'Nutrition for Hormonal Balance',
        content: 'Learn about foods that support hormonal health...',
        category: 'nutrition',
        tags: ['hormones', 'diet', 'wellness'],
        readingTime: 5,
        publishedDate: new Date('2024-01-15'),
        author: 'Dr. Sarah Johnson',
        medicalReviewed: true,
        ageRestriction: 'all',
      },
      {
        id: '2',
        title: 'Exercises for Pelvic Health',
        content: 'Strengthen your pelvic floor with these exercises...',
        category: 'exercise',
        tags: ['pelvic floor', 'fitness', 'postpartum'],
        readingTime: 8,
        publishedDate: new Date('2024-01-10'),
        author: 'Physical Therapist Jane Doe',
        medicalReviewed: true,
        ageRestriction: 'adult',
      },
      {
        id: '3',
        title: 'Managing PMS Naturally',
        content: 'Natural remedies for premenstrual syndrome...',
        category: 'mental_health',
        tags: ['pms', 'natural remedies', 'symptoms'],
        readingTime: 6,
        publishedDate: new Date('2024-01-05'),
        author: 'Nutritionist Emily Chen',
        medicalReviewed: true,
        ageRestriction: 'all',
      },
      {
        id: '4',
        title: 'Pregnancy Nutrition Guide',
        content: 'Essential nutrients for a healthy pregnancy...',
        category: 'pregnancy',
        tags: ['pregnancy', 'nutrition', 'prenatal'],
        readingTime: 10,
        publishedDate: new Date('2024-01-01'),
        author: 'OB/GYN Dr. Maria Rodriguez',
        medicalReviewed: true,
        ageRestriction: 'adult',
      },
      {
        id: '5',
        title: 'Understanding Your Cycle',
        content: 'A comprehensive guide to menstrual cycles...',
        category: 'education',
        tags: ['menstrual cycle', 'education', 'basics'],
        readingTime: 7,
        publishedDate: new Date('2023-12-20'),
        author: 'Health Educator Lisa Wang',
        medicalReviewed: true,
        ageRestriction: 'teen',
      },
      {
        id: '6',
        title: 'Mindfulness for Stress Relief',
        content: 'Techniques to manage stress and anxiety...',
        category: 'mental_health',
        tags: ['mindfulness', 'stress', 'meditation'],
        readingTime: 5,
        publishedDate: new Date('2023-12-15'),
        author: 'Psychologist Dr. Michael Brown',
        medicalReviewed: true,
        ageRestriction: 'all',
      },
    ];

    // Filter based on age
    const ageGroup = age < 18 ? 'teen' : 'adult';
    const filtered = sampleArticles.filter(article => 
      !article.ageRestriction || 
      article.ageRestriction === 'all' || 
      article.ageRestriction === ageGroup
    );

    setArticles(filtered);
    setFilteredArticles(filtered);
  };

  const filterArticles = () => {
    let filtered = [...articles];

    if (selectedCategory !== 'all') {
      filtered = filtered.filter(article => article.category === selectedCategory);
    }

    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(article =>
        article.title.toLowerCase().includes(query) ||
        article.tags.some(tag => tag.toLowerCase().includes(query)) ||
        article.author.toLowerCase().includes(query)
      );
    }

    setFilteredArticles(filtered);
  };

  const categories = [
    { id: 'all', label: 'All', icon: BookOpen },
    { id: 'nutrition', label: 'Nutrition', icon: Apple },
    { id: 'exercise', label: 'Exercise', icon: Heart },
    { id: 'mental_health', label: 'Mental Health', icon: Brain },
    { id: 'pregnancy', label: 'Pregnancy', icon: Baby },
    { id: 'education', label: 'Education', icon: BookOpen },
  ];

  const renderCategoryFilter = () => {
    return (
      <ScrollView 
        horizontal 
        showsHorizontalScrollIndicator={false}
        style={styles.categoryScroll}
        contentContainerStyle={styles.categoryContainer}
      >
        {categories.map((category) => {
          const Icon = category.icon;
          const isSelected = selectedCategory === category.id;
          
          return (
            <TouchableOpacity
              key={category.id}
              style={[
                styles.categoryButton,
                isSelected && styles.categoryButtonSelected,
                isSelected && { backgroundColor: theme.colors.primary + '20' },
              ]}
              onPress={() => setSelectedCategory(category.id)}
            >
              <Icon 
                size={20} 
                color={isSelected ? theme.colors.primary : theme.colors.textSecondary} 
              />
              <Text style={[
                styles.categoryLabel,
                isSelected && styles.categoryLabelSelected,
                isSelected && { color: theme.colors.primary },
              ]}>
                {category.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    );
  };

  const renderArticleItem = ({ item }: { item: ContentArticle }) => {
    const categoryIcons = {
      nutrition: Apple,
      exercise: Heart,
      mental_health: Brain,
      pregnancy: Baby,
      education: BookOpen,
    };

    const Icon = categoryIcons[item.category as keyof typeof categoryIcons] || BookOpen;

    return (
      <TouchableOpacity
        style={styles.articleCard}
        onPress={() => openArticle(item)}
      >
        <Card style={styles.articleContent}>
          <View style={styles.articleHeader}>
            <View style={[styles.categoryIcon, { backgroundColor: theme.colors.primary + '20' }]}>
              <Icon size={20} color={theme.colors.primary} />
            </View>
            <View style={styles.articleMeta}>
              <Text style={styles.articleCategory}>
                {item.category.replace('_', ' ').toUpperCase()}
              </Text>
              {item.medicalReviewed && (
                <View style={styles.reviewedBadge}>
                  <Text style={styles.reviewedText}>✓ Medical Reviewed</Text>
                </View>
              )}
            </View>
            <TouchableOpacity onPress={() => toggleBookmark(item.id)}>
              <Bookmark 
                size={20} 
                color={theme.colors.primary} 
                fill={isBookmarked(item.id) ? theme.colors.primary : 'transparent'}
              />
            </TouchableOpacity>
          </View>

          <Text style={styles.articleTitle} numberOfLines={2}>
            {item.title}
          </Text>

          <Text style={styles.articleExcerpt} numberOfLines={3}>
            {item.content}
          </Text>

          <View style={styles.articleFooter}>
            <View style={styles.readingTime}>
              <Clock size={14} color={theme.colors.textSecondary} />
              <Text style={styles.readingTimeText}>{item.readingTime} min read</Text>
            </View>
            <Text style={styles.articleAuthor}>By {item.author}</Text>
          </View>

          <View style={styles.tagsContainer}>
            {item.tags.slice(0, 3).map((tag, index) => (
              <View key={index} style={styles.tag}>
                <Text style={styles.tagText}>{tag}</Text>
              </View>
            ))}
          </View>
        </Card>
      </TouchableOpacity>
    );
  };

  const openArticle = (article: ContentArticle) => {
    // Navigate to article detail screen
    console.log('Open article:', article.id);
  };

  const toggleBookmark = (articleId: string) => {
    // Toggle bookmark in database
    console.log('Toggle bookmark:', articleId);
  };

  const isBookmarked = (articleId: string) => {
    // Check if bookmarked
    return false;
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadArticles();
    setRefreshing(false);
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <View style={styles.header}>
        <Text style={styles.title}>Health Library</Text>
        <Text style={styles.subtitle}>Trusted content for your wellness journey</Text>
      </View>

      <View style={styles.searchContainer}>
        <View style={[styles.searchBox, { backgroundColor: theme.colors.surface }]}>
          <Search size={20} color={theme.colors.textSecondary} />
          <TextInput
            style={[styles.searchInput, { color: theme.colors.text }]}
            placeholder="Search articles..."
            placeholderTextColor={theme.colors.textSecondary}
            value={searchQuery}
            onChangeText={setSearchQuery}
          />
          {searchQuery ? (
            <TouchableOpacity onPress={() => setSearchQuery('')}>
              <Text style={styles.clearButton}>✕</Text>
            </TouchableOpacity>
          ) : null}
        </View>
        <TouchableOpacity 
          style={[styles.filterButton, { backgroundColor: theme.colors.surface }]}
          onPress={() => {/* Open advanced filter */}}
        >
          <Filter size={20} color={theme.colors.primary} />
        </TouchableOpacity>
      </View>

      {renderCategoryFilter()}

      <FlatList
        data={filteredArticles}
        renderItem={renderArticleItem}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.articlesList}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <BookOpen size={48} color={theme.colors.gray} />
            <Text style={styles.emptyStateTitle}>No articles found</Text>
            <Text style={styles.emptyStateText}>
              Try a different search term or category
            </Text>
          </View>
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    padding: 16,
    paddingBottom: 8,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    opacity: 0.7,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  searchBox: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    marginLeft: 8,
    fontSize: 16,
  },
  clearButton: {
    fontSize: 18,
    opacity: 0.5,
    paddingHorizontal: 4,
  },
  filterButton: {
    padding: 10,
    borderRadius: 8,
  },
  categoryScroll: {
    marginBottom: 16,
  },
  categoryContainer: {
    paddingHorizontal: 16,
  },
  categoryButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginRight: 8,
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.1)',
  },
  categoryButtonSelected: {
    borderColor: 'transparent',
  },
  categoryLabel: {
    marginLeft: 6,
    fontSize: 14,
    fontWeight: '500',
  },
  categoryLabelSelected: {
    fontWeight: '600',
  },
  articlesList: {
    padding: 16,
    paddingTop: 0,
  },
  articleCard: {
    marginBottom: 16,
  },
  articleContent: {
    padding: 16,
  },
  articleHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  categoryIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  articleMeta: {
    flex: 1,
  },
  articleCategory: {
    fontSize: 12,
    fontWeight: '600',
    opacity: 0.7,
    marginBottom: 2,
  },
  reviewedBadge: {
    backgroundColor: '#E8F5E9',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    alignSelf: 'flex-start',
  },
  reviewedText: {
    fontSize: 10,
    color: '#2E7D32',
    fontWeight: '500',
  },
  articleTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 8,
    lineHeight: 24,
  },
  articleExcerpt: {
    fontSize: 14,
    lineHeight: 20,
    opacity: 0.8,
    marginBottom: 12,
  },
  articleFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  readingTime: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  readingTimeText: {
    fontSize: 12,
    opacity: 0.7,
    marginLeft: 4,
  },
  articleAuthor: {
    fontSize: 12,
    opacity: 0.7,
    fontStyle: 'italic',
  },
  tagsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  tag: {
    backgroundColor: 'rgba(0,0,0,0.05)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    marginRight: 8,
    marginBottom: 4,
  },
  tagText: {
    fontSize: 12,
    opacity: 0.7,
  },
  emptyState: {
    alignItems: 'center',
    padding: 48,
  },
  emptyStateTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyStateText: {
    textAlign: 'center',
    opacity: 0.7,
  },
});